$:.unshift File.join(File.dirname(__FILE__),"..","lib")
require 'test/unit'
require 'notify'

client=Notify::Client.new("stomp://localhost:61613",false)
msg=Notify::StringMessage.new("topic","messageType","ruby_group")
msg.body="hello world"

puts client.send_message(msg).to_s
class ClientTest < Test::Unit::TestCase
   
end
