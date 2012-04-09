$:.unshift File.join(File.dirname(__FILE__),"..","lib")
require 'test/unit'
require 'notify'
module NotifyTest
  class MockClient
      attr_accessor :max_string_size,:compress_size
  end 
end
class Message_Test< Test::Unit::TestCase
  def test_instance_message
    assert_raise(NotImplementedError){Notify::Message.new}
  end
  def test_bytes_message
    message=Notify::BytesMessage.new("topic","msgType","ruby")
    assert_not_nil(message)
    assert_equal("topic",message.topic)
    assert_equal("msgType",message.message_type)
    assert_equal("ruby",message.group_id)
    assert_equal(:bytes,message.type)
    assert_equal(5,message.priority)
    assert_equal(0,message.delivery_count)
    assert(!message.send_once?)  
    assert_equal(:gbk,message.charset)

    assert_nil(message[:a])
    message[:a]=1
    assert_equal(1,message[:a])
    message[:a]=999
    assert_equal(999,message[:a])
    message[:b]="hello notify"
    assert_equal("hello notify",message[:b])

    message.send_once=true
    assert(message.send_once?)
    message.body=[0,1,2,3]
    assert_equal([0,1,2,3],message.body)
    
    message.charset=:utf8
    assert_equal(:utf8,message.charset)
  end
  
  def test_string_message
    message=Notify::StringMessage.new("topic","msgType","ruby")
    assert_not_nil(message)
    assert_equal("topic",message.topic)
    assert_equal("msgType",message.message_type)
    assert_equal("ruby",message.group_id)
    assert_equal(:string,message.type)
    assert_equal(5,message.priority)
    assert_equal(0,message.delivery_count)
    assert(!message.send_once?)  
    assert_equal(:gbk,message.charset)

    assert_nil(message[:a])
    message[:a]=1
    assert_equal(1,message[:a])
    message[:a]=999
    assert_equal(999,message[:a])
    message[:b]="hello notify"
    assert_equal("hello notify",message[:b])

    message.send_once=true
    assert(message.send_once?)
    message.body="test body"
    assert_equal("test body",message.body)
    
    message.charset=:utf8
    assert_equal(:utf8,message.charset)
  end

  def test_copy
     message=Notify::StringMessage.new("topic","msgType","ruby")
     message.priority=10
     message.post_timeout=20
     message.dlq_time=10
     message.time_to_live=100
     message.post_delay_time=99
     message[:a]=1
     message.body="hello world"
     bytes_msg=Notify::BytesMessage.new
     bytes_msg.copy(message)
     
     assert_equal("topic",bytes_msg.topic)
     assert_equal("msgType",bytes_msg.message_type)
     assert_equal("ruby",bytes_msg.group_id) 
     assert_not_same(bytes_msg,message)
     assert_equal(1,bytes_msg[:a])
     assert_equal("hello world",bytes_msg.body)
     assert_equal(:bytes,bytes_msg.type)
     assert_equal(10,bytes_msg.priority)
     assert_equal(20,bytes_msg.post_timeout)
     assert_equal(10,bytes_msg.dlq_time)
     assert_equal(100,bytes_msg.time_to_live)
     assert_equal(99,bytes_msg.post_delay_time)
  end
  
   
  def test_stringmessage_to_send_type
     client=NotifyTest::MockClient.new
     client.max_string_size=1024
     client.compress_size=1024
     message=Notify::StringMessage.new("topic","msgType","ruby")
     message.priority=10
     message.post_timeout=20
     message.dlq_time=10
     message.time_to_live=100
     message.post_delay_time=99
     message[:a]=1
     message.body="hello world"
     assert_same(message,message.to_send_type(client))

     client.max_string_size=1
     send_message=message.to_send_type(client)
     assert_not_same(send_message,message)
     assert_equal("topic",send_message.topic)
     assert_equal("msgType",send_message.message_type)
     assert_equal("ruby",send_message.group_id) 
     assert_equal(1,send_message[:a])
     assert(send_message.body.class==Array)
     assert_equal(11,message.body.length)
     assert_equal("hello world",send_message.body.pack("c*"))
     assert_equal(:bytes,send_message.type)
     assert_equal(10,send_message.priority)
     assert_equal(20,send_message.post_timeout)
     assert_equal(10,send_message.dlq_time)
     assert_equal(100,send_message.time_to_live)
     assert_equal(99,send_message.post_delay_time)
  end

  def test_bytesmessage_to_send_type
  end
end
