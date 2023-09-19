local onEntityDamage = function(event)
    if event:getDamager():getType() == "CACTUS" then
        event:setCancelled(true)
    end
end

onEnable(
    function()
        lipt_event.on("EntityDamageByBlockEvent", onEntityDamage)
    end
)