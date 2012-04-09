# -*- coding: gb2312 -*-
require 'zlib'
module Notify
   def self.set_concrete_type(flag,type)
      if type==:string
         flag&0xFFFFFFFE | 0
      elsif type==:bytes
         flag&0xFFFFFFFE | 1
      else
        raise ArgumentError.new("未知消息类型:#{type}")
      end
   end
   
   def self.get_concrete_type(flag)
       flag&0x1==0? :string : :bytes 
   end
   
   def self.set_origin_type(flag,type)
       if type==:string
         flag&0xFFFFFFFD | (0 << 1)
       elsif type==:bytes
         flag&0xFFFFFFFD | (1 << 1)
       else
        raise ArgumentError.new("未知消息类型:#{type}")
       end
   end
   def self.get_origin_type(flag)
       (flag&0x2) >> 1==0? :string : :bytes 
   end
   def self.set_send_once(flag,bool)
       value=bool ? 1: 0
       flag & 0xFFFFFBFF | (value << 10)
   end
   def self.send_once?(flag)
        (flag & 0x400) >> 10 ==1 ? true : false
   end
   def self.set_compress_mode(flag,mode)
      flag & 0xFFFFFFE3 | (mode << 2) 
   end
   def self.get_compress_mode(flag)
      (flag & 0x1C) >> 2
   end
   def self.set_charset(flag,charset)
      flag & 0xFFFFC1F | (charset << 5)
   end
   def self.get_charset(flag)
      (flag & 0x3E0) >> 5
   end

   def self.compress(body)
      compressor=Zlib::Deflate.new
      result=compressor.deflate(body,Zlib::FINISH)
      compressor.close
      result
   end
end
