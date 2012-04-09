# -*- coding: gb2312 -*-
require 'notify/utils'
module Notify
   class Message
      attr_accessor :id,:topic,:message_type,:group_id,:committed,:priority,:time_to_live,
                    :dlq_time,:post_timeout,:post_delay_time,:user_defined_properties,:body
      attr_reader   :born_time,:flag,:gmt_created,:gmt_last_delivery,:delivery_count
      #阻止初始化
      def initialize(topic='',message_type='',group_id='')
        raise NotImplementedError.new("#{self.class.name}是抽象类，无法实例化，请使用具体子类")
      end

      def type=(type)
     
      end
      
      def []=(key,value)
          @user_defined_properties[key.to_s]=value
      end
      def property_names
          @user_defined_properties.keys
      end
      def [](key)
          @user_defined_properties[key.to_s]
      end
      def type
          Notify.get_concrete_type(@flag)
      end
      
      def send_once?
          Notify.send_once?(@flag)
      end
      def send_once=(send_once=false)
          @flag=Notify.set_send_once(@flag,send_once)
      end
      
      def charset
          value=Notify.get_charset(@flag)
          if value==0 then
             return :gbk
          elsif value==1 then
             return :utf8
          else
             raise TypeError.new("未知Charset类型")
          end
      end
      def charset=(charset=:gbk)
          if charset==:gbk then
             @flag=Notify.set_charset(@flag,0)
          elsif charset==:utf8 then
             @flag=Notify.set_charset(@flag,1)
          else
            raise TypeError.new("未知Charset类型,#{charset}")
          end
      end

      def copy(message)
          message.instance_variables.each do | var|
             name=var.split('@')[1]
             # p name if self.respond_to? "#{name}="
             instance_eval "#{var}=message.#{name}" if self.respond_to? "#{name}="
          end
      end
      
      def header
          result={}
          self.instance_variables.each do | var|
             name=convert(var.split("@")[1])
             result[name]=instance_variable_get(var)  
          end
          return result
      end

      def to_send_type(client)
        send_message=self
        if self.type==:string
             if @body.length < client.max_string_size
                 #ignore
             else
                 bytes_msg=Notify::BytesMessage.new
                 bytes_msg.copy(self)
                 if self.charset==:utf8
                    bytes_msg.body=@body.unpack("U*")
                 else
                    bytes_msg.body=@body.unpack("c*")
                 end
                 bytes_msg.charset=self.charset 
                 send_message=bytes_msg
             end   
        end
        if send_message.type==:bytes and send_message.body.length > client.compress_size
           send_message.body=Notify.compress(send_message.body)
           send_message.compress_mode(1)
        end
        send_message
     end
       
      protected
      def convert(name)
        result=name.split("_").map{|x| x.capitalize!}.join("")
        result[0]=result[0].chr.downcase
        return result
      end
      def inner_init(topic,message_type,group_id)
          @topic=topic
          @message_type=message_type
          @group_id=group_id
          @committed=false
          @priority=5
          @time_to_live=-1
          @dlq_time=-1
          @post_timeout=-1
          @post_delay_time=-1
          @born_time=Time.now.to_i
          @delivery_count=0
          @flag=0
          @user_defined_properties={}
      end
   end
   class StringMessage < Message
      def initialize(topic='',message_type='',group_id='')
         inner_init(topic,message_type,group_id)
         @flag=Notify.set_concrete_type(@flag,:string)
         @flag=Notify.set_origin_type(@flag,:string)
      end
   end
   class BytesMessage < Message
      def initialize(topic='',message_type='',group_id='')
         inner_init(topic,message_type,group_id)
         @flag=Notify.set_concrete_type(@flag,:bytes)
         @flag=Notify.set_origin_type(@flag,:bytes)
      end
   end
end
