# -*- coding: gb2312 -*-
require 'rubygems'
require 'stomp'
require 'notify/message'
require 'thread'
module Notify
  class SendResult
     attr_accessor :success,:error_message
     def initilize(success=false,error_message=nil)
        @success=success
        @error_message=error_message
     end
     def to_s
       "[SendResult success=#{@success} error_message=#{@error_message}]"
     end
  end
  class Client
     attr_accessor :max_string_size,:compress_size
     def initialize(uri,reliable=true)
        if uri=~/^stomp:\/\/(.*):(\d+)/
           @host=$1
           @port=$2.to_i
        else
           raise RuntimeError("非法的URI，必须是stomp://host:port格式")
        end
        @max_string_size=1024
        @compress_size=1024
        @stomp=Stomp::Client.new("notify","notify",@host,@port,reliable)
        @mutex=Mutex.new
        @not_done=ConditionVariable.new 
     end

     def max_string_size=(value)
       raise ArgumentError if value<=0
       @max_string_size=value
     end

     def send_message(message)
        check_message(message)
        send_message=message.to_send_type(self)
        destination="/#{send_message.topic}/#{send_message.message_type}"
        
        result=SendResult.new
        #异步回调
        if block_given?
          listener=lambda{|r| yield r}
          @stomp.send(destination,send_message.body,send_message.header) do | response_msg|
             result.success,result.error_message=(response_msg.headers["result"]=="true"),response_msg.body
             listener.call(result)
          end
        #同步调用
        else
           not_done=true
           @mutex.synchronize{
              @stomp.send(destination,send_message.body,send_message.header) do | response_msg|
                  @mutex.synchronize{
                     result.success,result.error_message=(response_msg.headers["result"]=="true"),response_msg.body
                     not_done=false
                     @not_done.signal
                  }
               end
               @not_done.wait(@mutex) while not_done
           }         
           return result
        end
     end

     def close
        @stomp=close
     end
    
     private
     def check_message(message)
       check_string(message.topic,"topic")
       check_string(message.message_type,"message_type")         
       check_string(message.group_id,"group_id")         
       raise ArgumentError.new("nil body") if message.body.nil?         
     end
     
     def check_string(str,name)
       raise ArgumentError.new("#{name}为nil") if str.nil?
       raise ArgumentError.new("#{name}含有空白字符") if str.index(/\s/)  
     end

     
  end
end
