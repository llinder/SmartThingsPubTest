/**
 *  Nest Thermostat (device type)
 *
 *  Author: Juan Pablo Risso (juan@smartthings.com)
 *
 *  Date: 2015-02-05
 *
 *  To-Do:
 *		- Concent Messege (platform) 
 *		- Change away (once message is done)
 *
 */

 // for the UI
metadata {
    definition (name: "Nest Thermostat", namespace: "smartthings", author: "juano23@gmail.com") {
        capability "Relative Humidity Measurement"
        capability "Thermostat"
        capability "Polling"        
        capability "Temperature Measurement"

        attribute "leafinfo", "string"
        attribute "presence", "string"
        attribute "emergencyheat", "string"
        attribute "canheat", "string"
        attribute "cancool", "string"
        
        command "mode"
        command "coolup"
        command "cooldown"
        command "heatup"
        command "heatdown"      
    }

    simulator {
        // TODO: define status and reply messages here
    }

    tiles {
        valueTile("temperature", "device.temperature", width: 1, height: 1, canChangeIcon: true) {
            state("temperature", label: '${currentValue}°', unit:"F", backgroundColors: [
            		[value: '', color: "#ffffff"],
                    [value: 31, color: "#153591"],
                    [value: 44, color: "#1e9cbb"],
                    [value: 59, color: "#90d2a7"],
                    [value: 74, color: "#44b621"],
                    [value: 84, color: "#f1d801"],
                    [value: 95, color: "#d04e00"],
                    [value: 96, color: "#bc2323"]
                ]
            )
        }
        standardTile("lowarrowup", "device.lowarrowup", inactiveLabel: false, decoration: "flat") {
            state "default", label:'', action:"heatup", backgroundColor:"#ffffff", icon:"st.thermostat.thermostat-up"
        }
        standardTile("higharrowup", "device.higharrowup", inactiveLabel: false, decoration: "flat") {
            state "default", label:'', action:"coolup", backgroundColor:"#ffffff", icon:"st.thermostat.thermostat-up"
        }        
		standardTile("thermostatMode", "device.thermostatMode", inactiveLabel: false, decoration: "flat") {
            state("waiting", label:'${name}', icon: "st.unknown.unknown.unknown")
			state("heat-cool", label:'${name}', action:"mode", icon: "st.tesla.tesla-hvac")
            state("off", action:"mode", icon: "st.thermostat.heating-cooling-off")
            state("cool", action:"mode", icon: "st.thermostat.cool")
            state("heat", action:"mode", icon: "st.thermostat.heat")
            state("offline", label:'${name}', icon: "st.illuminance.illuminance.dark")
            state("away", label:'${name}', icon: "st.nest.nest-away")
            state("emergency", icon: "st.thermostat.emergency-heat")
            state("auto-away", label:'${name}', icon: "st.nest.nest-away")
			state("rushhour", label:'rush hour', icon: "st.Home.home1")            
        }
        valueTile("heatingSetpoint", "device.heatingSetpoint", inactiveLabel: false) {
            state "default", label:'${currentValue}°', unit:"F", backgroundColor:"#ffffff", icon:"st.appliances.appliances8"
        }
        valueTile("coolingSetpoint", "device.coolingSetpoint", inactiveLabel: false) {
            state "default", label:'${currentValue}°', unit:"F", backgroundColor:"#ffffff", icon:"st.appliances.appliances8"
        }        
        valueTile("humidity", "device.humidity", inactiveLabel: false) {
            state "default", label:'${currentValue}% Humidity', unit:"Humidity"
        }
        standardTile("lowarrowdown", "device.lowarrowdown", inactiveLabel: false, decoration: "flat") {
            state "default", label:'', action:"heatdown", backgroundColor:"#ffffff", icon:"st.thermostat.thermostat-down"
        }
        standardTile("higharrowdown", "device.higharrowdown", inactiveLabel: false, decoration: "flat") {
            state "default", label:'', action:"cooldown", backgroundColor:"#ffffff", icon:"st.thermostat.thermostat-down"
        } 
		standardTile("leafinfo", "device.leafinfo", inactiveLabel: false, decoration: "flat") {
            state "yes", label:'', icon: "st.nest.nest-leaf"
            state "no", label:'', icon: "st.illuminance.illuminance.dark"
        }
        standardTile("presence", "device.presence", inactiveLabel: false, decoration: "flat") {
            state "home", label:'${name}', action:"away", icon: "st.nest.nest-home"
            state "away", label:'${name}', action:"present", icon: "st.nest.nest-away"
            state "auto-away", label:'${name}', action:"present", icon: "st.nest.nest-away"
        }
		standardTile("emergencyheat", "device.emergencyheat", inactiveLabel: false, decoration: "flat") {
            state "yes", label:'', icon: "st.thermostat.emergency-heat"
            state "no", label:'', icon: "st.illuminance.illuminance.dark"
        }        
		standardTile("thermostatFanMode", "device.thermostatFanMode", inactiveLabel: false, decoration: "flat") {
            state "auto", label:'${name}', action:"thermostat.fanOn", icon: "st.Appliances.appliances11"
            state "on", label:'${name}', action:"thermostat.fanCirculate", icon: "st.Appliances.appliances11"
            state "circulate", label:'${name}', action:"thermostat.fanAuto", icon: "st.Appliances.appliances11"
        }
		standardTile("refresh", "device.thermostatMode", inactiveLabel: false, decoration: "flat") {
            state "default", action:"polling.poll", icon:"st.secondary.refresh"
        }
        main "temperature"
    	details(["temperature", "lowarrowup", "higharrowup", "thermostatMode", "heatingSetpoint", "coolingSetpoint", "humidity", "lowarrowdown", "higharrowdown", "presence", "leafinfo", "emergencyheat", "refresh"])
    }
}

// handle commands

def away() {
    setPresence('away')
}

def present() {
    setPresence('present')
}

def setPresence(status) {
    log.debug "Presence: $status"
    parent.presence(status)
}

def poll() {
    log.debug "Executing 'poll'"
    parent.poll()
}

def heatup() {
    parent.poll()
    log.trace "Heat up"
    def max
    def heatingvalue = device.latestState('heatingSetpoint').value as BigDecimal 
    def targetvalue = heatingvalue
    def scale= getTemperatureScale().toLowerCase()   
    if (scale == "f") {
    	targetvalue = heatingvalue + 1 
        max = 90
    } else {  
    	targetvalue = heatingvalue + 0.5
        max = 32
    }         
   	if(targetvalue <= max) {    
        def latestThermostatMode = device.latestState('thermostatMode').stringValue     
        switch (latestThermostatMode) {
            case "heatcool":
            	sendEvent(name:"heatingSetpoint", value: targetvalue) 
                parent.temp(device.deviceNetworkId, "target_temperature_low_$scale", targetvalue)
                break;
            case "heat":
                sendEvent(name:"heatingSetpoint", value: targetvalue) 
                parent.temp(device.deviceNetworkId, "target_temperature_$scale", targetvalue)
                break;  
            default:
                parent.sendNotification("This action is not available in mode $latestThermostatMode")
                break;        
        }  
	} else {
    	parent.sendNotification("The value is out of the allowed range")
    }       
}

def coolup() {
    parent.poll()
    log.trace "Cool up"
    def max
    def coolingvalue = device.latestState('heatingSetpoint').value as BigDecimal 
    def targetvalue = coolingvalue
    def scale= getTemperatureScale().toLowerCase()   
    if (scale == "f") {
    	targetvalue = coolingvalue + 1 
        max = 90
    } else {  
    	targetvalue = coolingvalue + 0.5
        max = 32
    }         
   	if(targetvalue <= max) {     
        def latestThermostatMode = device.latestState('thermostatMode').stringValue     
        switch (latestThermostatMode) {
            case "heatcool":
            	sendEvent(name:"heatingSetpoint", value: targetvalue) 
                parent.temp(device.deviceNetworkId, 'target_temperature_high_f', targetvalue)
                break;
            case "cool":
                sendEvent(name:"heatingSetpoint", value: targetvalue) 
                parent.temp(device.deviceNetworkId, 'target_temperature_f', targetvalue)
                break;  
            default:
                parent.sendNotification("This action is not available in mode $latestThermostatMode")
                break;        
        }  
	} else {
    	parent.sendNotification("The value is out of the allowed range")
    }
}

def heatdown() {
    parent.poll()
    log.trace "Heat down"
    def min
    def heatingvalue = device.latestState('heatingSetpoint').value as BigDecimal 
    def targetvalue = heatingvalue
    def scale= getTemperatureScale().toLowerCase()   
    if (scale == "f") {
    	targetvalue = heatingvalue - 1 
        min = 50
    } else {  
    	targetvalue = heatingvalue - 0.5
        mim = 9
    }         
   	if(targetvalue >= min) {   
        def latestThermostatMode = device.latestState('thermostatMode').stringValue     
        switch (latestThermostatMode) {
            case "heatcool":
            	sendEvent(name:"heatingSetpoint", value: targetvalue) 
                parent.temp(device.deviceNetworkId, 'target_temperature_low_f', targetvalue)
                break;
            case "heat":
                sendEvent(name:"heatingSetpoint", value: targetvalue) 
                parent.temp(device.deviceNetworkId, 'target_temperature_f', targetvalue)
                break;  
            default:
                parent.sendNotification("This action is not available in mode $latestThermostatMode")
                break;        
        }  
	} else {
    	parent.sendNotification("The value is out of the allowed range")
    }        
}

def cooldown() {
    parent.poll()
    log.trace "Cool down"
    def min
    def coolingvalue = device.latestState('heatingSetpoint').value as BigDecimal 
    def targetvalue = coolingvalue
    def scale= getTemperatureScale().toLowerCase()   
    if (scale == "f") {
    	targetvalue = coolingvalue + 1 
        min = 50
    } else {  
    	targetvalue = coolingvalue + 0.5
        min = 9
    }         
   	if(targetvalue >= min) {     
        def latestThermostatMode = device.latestState('thermostatMode').stringValue     
        switch (latestThermostatMode) {
            case "heatcool":
            	sendEvent(name:"heatingSetpoint", value: targetvalue) 
                parent.temp(device.deviceNetworkId, 'target_temperature_high_f', targetvalue)
                break;
            case "cool":
                sendEvent(name:"heatingSetpoint", value: targetvalue) 
                parent.temp(device.deviceNetworkId, 'target_temperature_f', targetvalue)
                break;  
            default:
                parent.sendNotification("This action is not available in mode $latestThermostatMode")
                break;        
        }  
	} else {
    	parent.sendNotification("The value is out of the allowed range")
    }     
}

def mode() {
    log.trace "Switch Mode"
    parent.poll()
    def canheatvalue = device.latestState('canheat').stringValue
    def cancoolvalue = device.latestState('cancool').stringValue 
    def latestThermostatMode = device.latestState('thermostatMode').stringValue
    if (latestThermostatMode == "off" && canheatvalue == "yes")
    	parent.mode(device.deviceNetworkId,"heat")
    else if ((latestThermostatMode == "heat" && cancoolvalue == "yes") || (latestThermostatMode == "off" && canheatvalue == "no"))
    	parent.mode(device.deviceNetworkId,"cool")  
    else if (latestThermostatMode == "cool" && canheatvalue == "yes")
    	parent.mode(device.deviceNetworkId,"heat-cool")  
    else if (latestThermostatMode == "heat-cool" || (latestThermostatMode == "heat" && cancoolvalue == "no") || (latestThermostatMode == "cool" && canheatvalue == "no"))
    	parent.mode(device.deviceNetworkId,"off")  
}