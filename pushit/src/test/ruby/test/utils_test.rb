$:.unshift File.join(File.dirname(__FILE__),"..","lib")
require 'test/unit'
require 'notify'

class NotifyUtils_Test< Test::Unit::TestCase
   def setup
     @flag=0
   end
   def test_get_set_concrete_type
     @flag=Notify.set_concrete_type(@flag,:string)
     assert_equal(:string,Notify.get_concrete_type(@flag))
     
     @flag=Notify.set_concrete_type(@flag,:bytes)
     assert_equal(:bytes,Notify.get_concrete_type(@flag))
     assert_raise(ArgumentError){@flag=Notify.set_concrete_type(@flag,:unknown)}
   end
   
   def test_get_set_origin_type
     @flag=Notify.set_origin_type(@flag,:string)
     assert_equal(:string,Notify.get_origin_type(@flag))
     
     @flag=Notify.set_origin_type(@flag,:bytes)
     assert_equal(:bytes,Notify.get_origin_type(@flag))
     assert_raise(ArgumentError){@flag=Notify.set_origin_type(@flag,:unknown)}
   end
   
   def test_get_set_send_once
     @flag=Notify.set_send_once(@flag,true)
     assert(Notify.send_once?(@flag))
     @flag=Notify.set_send_once(@flag,false)
     assert(!Notify.send_once?(@flag))
     @flag=Notify.set_send_once(@flag,1)
     assert(Notify.send_once?(@flag))
    
   end

   def test_get_set_compress_mode
     @flag=Notify.set_compress_mode(@flag,0)
     assert_equal(0,Notify.get_compress_mode(@flag))
     @flag=Notify.set_compress_mode(@flag,1)
     assert_equal(1,Notify.get_compress_mode(@flag))
     @flag=Notify.set_compress_mode(@flag,3)
     assert_equal(3,Notify.get_compress_mode(@flag))
   end

   def test_get_set_charset
     @flag=Notify.set_charset(@flag,0)
     assert_equal(0,Notify.get_charset(@flag))
     @flag=Notify.set_charset(@flag,1)
     assert_equal(1,Notify.get_charset(@flag))
     @flag=Notify.set_charset(@flag,31)
     assert_equal(31,Notify.get_charset(@flag))
   end
end

