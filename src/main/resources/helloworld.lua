server:getConsoleSender():sendMessage("Hello, World!")

local message = component({
  text = "",
  underlined = true,
  extra = {
    {color = "#FF0000", text = "H"},
    {color = "#FF7F00", text = "e"},
    {color = "#FFFF00", text = "l"},
    {color = "#7FFF00", text = "l"},
    {color = "#00FF00", text = "o"},
    {color = "#FF007F", text = "!"}
  }
})

local onEvent = function(event)
    event:getPlayer():sendMessage(message)
end

lipt_event.on("PlayerJoinEvent", onEvent)
