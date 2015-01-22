/**
 *  TCP Bulbs.groovy
 *
 *  Author: todd@wackford.net
 *  Date: 2014-03-07
 *
 *
 *****************************************************************
 *                       Changes
 *****************************************************************
 *
 *  Change 1:	2014-03-10
 *				Documented Header
 *
 *  Change 2:	2014-03-15
 *				Fixed bug where we weren't coming on when changing 
 *				levels down.
 *
 *  Change 3:   2014-04-02 (lieberman)	
 *              Changed sendEvent() to createEvent() in parse()
 *
 *  Change 4:	2014-04-12 (wackford)
 *				Added current power usage tile 
 *
 *  Change 5:	2014-09-14 (wackford)
 *				a. Changed createEvent() to sendEvent() in parse() to
 *				   fix tile not updating.
 *				b. Call IP checker for DHCP environments from refresh. Parent
 *				   service manager has method to call every 5 minutes too.
 *
 *  Change 6:	2014-10-17 (wackford)
 *				a. added step size input to settings of device
 *				b. added refresh on udate
 *				c. added uninstallFromChildDevice to handle removing from settings
 *				d. Changed to allow bulb to 100%, was possible to get past logic at 99
 *
 *****************************************************************
 *                       Code
 *****************************************************************
 */
 // for the UI
metadata {
	definition (name: "TCP Bulb", namespace: "wackford", author: "Todd Wackford") {
		capability "Switch"
		capability "Polling"
		capability "Power Meter"
		capability "Refresh"
		capability "Switch Level"

		attribute "stepsize", "string"
        
		command "levelUp"
		command "levelDown"
        command "on"
        command "off"
	}

	simulator {
		// TODO: define status and reply messages here
	}
    
    preferences {
		input "stepsize", "number", title: "Step Size", description: "Dimmer Step Size", defaultValue: 5
	}
    
	tiles {
		standardTile("switch", "device.switch", width: 2, height: 2, canChangeIcon: true) {
			state "on", label:'${name}', action:"off", icon:"st.Lighting.light14", backgroundColor:"#79b821"
			state "off", label:'${name}', action:"on", icon:"st.Lighting.light14", backgroundColor:"#ffffff"
		}
		controlTile("levelSliderControl", "device.level", "slider", height: 1, width: 2, inactiveLabel: false) {
			state "level", action:"switch level.setLevel"
		}
		standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat") {
			state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
		}
		valueTile("level", "device.level", inactiveLabel: false, decoration: "flat") {
			state "level", label: 'Level ${currentValue}%'
		}
        standardTile("lUp", "device.switchLevel", inactiveLabel: false,decoration: "flat", canChangeIcon: false) {
            state "default", action:"levelUp", icon:"st.illuminance.illuminance.bright"
        }
        standardTile("lDown", "device.switchLevel", inactiveLabel: false,decoration: "flat", canChangeIcon: false) {
            state "default", action:"levelDown", icon:"st.illuminance.illuminance.light"
        }
        valueTile( "power", "device.power", inactiveLabel: false, decoration: "flat") {
            state "power", label: '${currentValue}', unit:"", backgroundColor:"#ffffff"
        }

		main(["switch"])
		details(["switch", "lUp", "lDown", "levelSliderControl", "level" , "power", "refresh" ])
	}
}

// parse events into attributes
def parse(description) {
	log.debug "parse() - $description"
	def results = []
	
    if ( description == "updated" )
    	return
        
	if (description?.name && description?.value)
	{
		results << createEvent(name: "${description?.name}", value: "${description?.value}")
	}
}

// handle commands
def on() {
	log.debug "Executing 'on'"
    sendEvent(name:"switch",value:on)
	parent.on(this)
}

def off() {
	log.debug "Executing 'off'"
    sendEvent(name:"switch",value:off)
	parent.off(this)
}

def levelUp() {
	def level = device.latestValue("level") as Integer ?: 0
    def step = state.stepsize as float
    
    level+= step
    
    if ( level > 100 )
    	level = 100
    
    setLevel(level)
}

def levelDown() {
	def level = device.latestValue("level") as Integer ?: 0
    def step = state.stepsize as float
    
    level-= step
    
	if ( level <  1 )
    	level = 1
    
    setLevel(level)
}

def setLevel(value) {
	def level = value as Integer

    if (( level > 0 ) && ( level <= 100 ))
    	on()
    else
    	off()
    
    sendEvent( name: "level", value: level )
    sendEvent( name: "switch.setLevel", value:level )
	parent.setLevel( this, level )
}

def poll() {
	log.debug "Executing poll()"
	parent.poll(this)
}

def refresh() {
	log.debug "Executing refresh()"
    parent.updateGatewayIP()
	parent.poll(this)
}

def installed() {
	initialize()
}

def updated() {
    initialize()
    refresh()
}

def initialize() {
	if ( !settings.stepsize )
        state.stepsize = 10 //set the default stepsize
    else
    	state.stepsize = settings.stepsize
}

def uninstalled() {
	log.debug "Executing 'uninstall' in child"
    parent.uninstallFromChildDevice(this)
}
