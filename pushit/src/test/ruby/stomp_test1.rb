require 'rubygems'
require 'stomp'

include Stomp

client=Client.new("notify","notify")
header={"groupId"=>"ruby_group","committed"=>true,"flag"=>0,"bornTime"=>1000}
client.send("/Topic/MessageType","hello world",header) do |resp|
    p resp
end

sleep(5)
client.close

