/**
 *  Lighting Automation
 *
 *  Copyright 2015 Bob Florian
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
definition(
	name: "Lighting Automation",
	namespace: "smartthings/uxv2/lighting",
	author: "SmartThings",
	description: "Prototype lighting automation app uses dynamically updating preferences page to configure actions and triggers. Also automatically names app.",
	category: "",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/ModeMagic/Cat-ModeMagic.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/ModeMagic/Cat-ModeMagic@2x.png",
	iconX3Url: "https://s3.amazonaws.com/smartapp-icons/ModeMagic/Cat-ModeMagic@3x.png",
)

preferences {
	page name: "mainPage", title: "Automate Lights & Switches", install: false, uninstall: true, nextPage: "namePage"
	page name: "namePage", title: "Automate Lights & Switches", install: true, uninstall: true
	page(name: "timeIntervalInput", title: "Only during a certain time") {
		section {
			input "starting", "time", title: "Starting", required: false
			input "ending", "time", title: "Ending", required: false
		}
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	unschedule()
	initialize()
}

def initialize() {
	if (!overrideLabel) {
		app.updateLabel(defaultLabel())
	}

	switch(trigger) {
		case "Motion":
			subscribe(motionSensors, "motion.$motionState", triggerHandler)
			if (motionStops) {
				subscribe(motionSensors, "motion.inactive", motionStopHandler)
			}
			break
		case "Open/Close":
			subscribe(contactSensors, "contact.$contactState", triggerHandler)
			if (contactCloses) {
				subscribe(contactSensors, "contact.${contactState == 'closed' ? 'open' : 'closed'}", stopHandler)
			}
			break
		case "Presence":
			if (presenceState == "present") {
				subscribe(presenceSensors, "presence.present", triggerHandler)
			}
			else {
				subscribe(presenceSensors, "presence.not present", departureHandler)
			}
			break
		case "Switch":
			subscribe(switchTrigger, "switch.$switchState", triggerHandler)
			if (switchToggle) {
				subscribe(switchTrigger, "switch.${switchState == 'off' ? 'on' : 'off'}", stopHandler)
			}
			break
		case "Button":
			subscribe(buttonTrigger, "button", buttonHandler)
			break
		case "At Sunrise":
			subscribe(location, "sunrise", triggerHandler)
            if (sunsetFollowup) {
            	subscribe(location, "sunset", stopHandler)
            }
			break
		case "At Sunset":
			subscribe(location, "sunset", triggerHandler)
            if (sunriseFollowup) {
            	subscribe(location, "sunrise", stopHandler)
            }
            break
		case "At a Specific Time":
			schedule(scheduledTime, triggerHandler)
            if (scheduledTimeFollowup) {
            	schedule(scheduledTimeFollowup, stopHandler)
            }
			break
		case "When Mode Changes":
			subscribe(location, modeChangeHandler)
			break
		case "Power Allowance":
			subscribe(lights, "switch.on", powerAllowanceOnHandler)
			subscribe(lights, "switch.off", powerAllowanceOffHandler)
	}

}

// PRIMARY EVENT HANDLERS
//
// Generic handler for primary events
def triggerHandler(evt = [:]) {
	log.trace "triggerHandler($evt.name: $evt.value)"
	if (allOk) {
		startAction(evt)
	}

	if (motionStopsTime || contactClosesTime || powerAllowanceTime) {
		unschedule("stopAction", [cassandra:true])
	}
}

def modeChangeHandler(evt) {
	log.trace "modeChangeHandler($evt.name: $evt.value)"
	if (allOk) {
        if (evt.value in triggerModes) {
            startAction(evt)
        }
        else if (modeChangeFollowup && !evt.value in triggerModes) {
        	stopAction(evt)
        }
	}
}

def powerAllowanceOnHandler(evt) {
	log.trace "powerAllowanceOnHandler($evt.name: $evt.value)"
	if (allOk) {
		runIn(powerAllowanceTime * 60, startAction, [cassandra: true])
	}
}

def powerAllowanceOffHandler(evt) {
	unschedule("startAction", [cassandra:true])
}

def buttonHandler(evt)
{
	def data = evt.jsonData
	log.trace "buttonHandler($evt.name: $evt.value, $data)"
	if (allOk) {
    	if (data.buttonNumber == buttonNumber as Integer) {
        	if (evt.value == buttonAction) {
                if (state.pushed) {
                    log.trace "stopAction()"
                    stopAction(evt)
                    state.pushed = false
                }
                else {
                    log.trace "startAction()"
                    startAction(evt)
                    state.pushed = true
                }
            }
        }
	}
}

def departureHandler(evt) {
	log.trace "departureHandler($evt.name: $evt.value)"
	if (allOk) {
		def stillPresent = presenceSensors.find{it.currentValue("presence") == "present"}
		if (stillPresent) {
			log.debug "$stillPresent.displayName is still present"
		}
		else {
			startAction(evt)
		}
	}
}


// FOLLOWUP EVENT HANDLERS
//
// Generic handler for follow-up events
def stopHandler(evt = [:]) {
	log.trace "stopHandler($evt.name: $evt.value)"
	if (allOk) {
		if (trigger == "Open/Close") {
			if (contactClosesTime) {
				runIn(contactClosesTime * 60, stopAction, [cassandra: true])
			}
			else {
				stopAction(evt)
			}
		}
		else {
			stopAction(evt)
		}
	}
}

def motionStopHandler(evt) {
	log.trace "motionStopHandler($evt.name: $evt.value)"
	if (allOk) {
		def stillMoving = motionSensors.find{it.currentValue("motion") == "active"}
		if (stillMoving) {
			log.debug "$stillMoving.displayName still seeing motion"
		}
		else {
			if (motionStopsTime) {
				runIn(motionStopsTime * 60, stopAction, [cassandra: true])
			}
			else {
				stopAction(evt)
			}
		}
	}
}


// ACTION DISPATCHERS
//
// Action dispatcher for primary events
def startAction(evt = [:]) {
	switch(action) {
		case "on":
			log.debug "on()"
			lights.on()
			break
		case "off":
			log.debug "off()"
			lights.off()
			break
		case "level":
			lights.each {
				if (it.hasCapability("Switch Level")) {
					log.debug("setLevel($level)")
					it.setLevel(level as Integer)
				}
				else {
					it.on()
				}
			}
			break
		case "color":
			setColor()
			break
	}
}

// Action dispatcher for followup events
def stopAction(evt = [:]) {
	log.trace "stopAction()"
	switch(action) {
		case "on":
		case "level":
        case "color":
			log.debug "off()"
			lights.off()
			break
		case "off":
			log.debug "on()"
			lights.on()
			break
	}
}


// DYNAMIC PREFERENCE PAGES
//
def mainPage() {
	dynamicPage(name: "mainPage") {
		section {
			lightInputs()
			actionInputs()
		}
		triggerInputs()
		otherInputs()
	}
}

def namePage() {
	if (!overrideLabel) {
		app.updateLabel(defaultLabel())
	}
	dynamicPage(name: "namePage") {

		if (overrideLabel) {
			section {
				label title: "Automation name", required: false
			}
		}
		else {
			section("Automation name") {
				paragraph app.label
			}
		}
		section {
			input "overrideLabel", "bool", title: "Override automation name", defaultValue: "false", required: "false", submitOnChange: true
		}
	}
}


// IMPLEMENTATION METHODS
//
private lightInputs() {
	input "lights", "capability.switch", title: "Select lights to control", multiple: true, submitOnChange: true
}

private actionMap() {
    def map = [on: "Turn On", off: "Turn Off"]
    if (lights.find{it.hasCapability("Switch Level")} != null) {
        map.level = "Turn On & Set Level"
    }
    if (lights.find{it.hasCapability("Color Control")} != null) {
        map.color = "Turn On & Set Color"
    }
    map
}

private actionOptions() {
    actionMap().collect{[(it.key): it.value]}
}

private actionInputs() {
	if (lights) {
		input "action", "enum", title: "Select action", options: actionOptions(), submitOnChange: true
		if (action == "color") {
			input "color", "enum", title: "Color", required: false, multiple:false, options: [
				["Soft White":"Soft White - Default"],
				["White":"White - Concentrate"],
				["Daylight":"Daylight - Energize"],
				["Warm White":"Warm White - Relax"],
				"Red","Green","Blue","Yellow","Orange","Purple","Pink"]

		}
		if (action == "level" || action == "color") {
			input "level", "enum", title: "Dimmer Level", options: [[10:"10%"],[20:"20%"],[30:"30%"],[40:"40%"],[50:"50%"],[60:"60%"],[70:"70%"],[80:"80%"],[90:"90%"],[100:"100%"]], defaultValue: "80"
		}
	}
}

def triggerInputs() {
	if (settings.action) {
		section {
        	def actionTitle = actionMap()[action]
        	def actionLabel = actionTitle[0] + actionTitle[1..-1].toLowerCase()
			
            def triggerOptions = ["Motion", "Open/Close", "Presence", "Switch", "Button", "At Sunrise", "At Sunset", "At a Specific Time", "When Mode Changes"]
			if (settings.action == "off") {
				triggerOptions += ["Power Allowance"]
			}
			input "trigger", "enum", title: "Select trigger", options: triggerOptions, submitOnChange: true
			switch(trigger) {
				case "Motion":
					input "motionSensors", "capability.motionSensor", title: "Motion sensors", multiple: true, required: false
					input "motionState", "enum", title: "$actionLabel when", options: [["active":"When motion starts"], ["inactive":"When motion stops"]], defaultValue: "active", submitOnChange: true
					if (motionState != "inactive") {
						input "motionStops", "bool", title: "Turn ${action == "off" ? 'on' : 'off'} after motion stops", defaultValue: "false", required: false, submitOnChange: true
						if (motionStops) {
							input "motionStopsTime", "number", title: "After this number of minutes", required: false
						}
					}
					break
				case "Open/Close":
					input "contactSensors", "capability.contactSensor", title: "Open/close sensors", multiple: true, required: false
					input "contactState", title: "$actionLabel when", "enum", options: [["open":"When opened"], ["closed":"When closed"]], defaultValue: "open", submitOnChange: true
                    input "contactCloses", "bool", title: "Turn ${action == "off" ? 'on' : 'off'} when ${contactState == 'closed' ? 'opened' : 'closed'}", defaultValue: "false", required: false, submitOnChange: true
                    if (contactCloses) {
                        input "contactClosesTime", "number", title: "After this number of minutes", required: false
                    }
					break
				case "Presence":
					input "presenceSensors", "capability.presenceSensor", title: "Presence sensors", multiple: true, required: false
					input "presenceState", "enum", title: "$actionLabel when", options: [["present":"Someone arrives"], ["not present":"Everyone leaves"]], required: false, submitOnChange: true
					break
				case "Switch":
					input "switchTrigger", "capability.switch", title: "Master switch", required: false
					input "switchState", "enum", title: "$actionLabel when", options: [["on":"Turned on"], ["off":"Turned"]], defaultValue: "on", required: false, submitOnChange: true
					input "switchToggle", "bool", title: "Turn ${action == "off" ? 'on' : 'off'} as well as ${action}", defaultValue: "true", required: false
					break
				case "Button":
					input "buttonTrigger", "capability.button", title: "Button device", required: false
					input "buttonNumber", "enum", title: "Button number", options: [[1:"One"], [2:"Two"], [3:"Three"], [4:"Four"]], default: 1, required: false
					input "buttonAction", "enum", title: "Button action", options: ["pushed":"Pushed", "held":"Held"], default: "pushed", required: false
					input "buttonToggle", "bool", title: "Toggle on & off", defaultValue: "true", required: false
					break
				case "At Sunrise":
                	input "sunsetFollowup", "bool", title: "Also turn ${action == "off" ? 'on' : 'off'} at sunset", defaultValue: "false", required: false
					break
				case "At Sunset":
					input "sunriseFollowup", "bool", title: "Also turn ${action == "off" ? 'on' : 'off'} at sunrise", defaultValue: "false", required: false
					break
                case "At a Specific Time":
					input "scheduledTime", "time", title: "$actionLabel at", required: false
                    input "scheduledTimeFollowup", "time", title: "Also turn ${action == "off" ? 'on' : 'off'} at", defaultValue: "false", required: false, submitOnChange: true
					break
				case "When Mode Changes":
					input "triggerModes", "mode", title: "$actionLabel when mode changes to", multiple: true, required: false
					input "modeChangeFollowup", "bool", title: "Also turn ${action == "off" ? 'on' : 'off'} when mode changes back", defaultValue: "false", required: false
					break
				case "Power Allowance":
					input "powerAllowanceTime", "number", title: "After this number of minutes", required: false
					break
			}
		}
	}
}

def otherInputs() {
	if (settings.trigger) {
    	def timeLabel = timeIntervalLabel()
		section(title: "More options", hidden: hideOptionsSection(), hideable: true) {
        	def timeBasedTrigger = trigger in ["At Sunrise", "At Sunset", "At a Specific Time"]
            log.trace "timeBasedTrigger: $timeBasedTrigger"
        	if (!timeBasedTrigger) {
				href "timeIntervalInput", title: "Only during a certain time", description: timeLabel ?: "Tap to set", state: timeLabel ? "complete" : "incomplete"
			}
            
			input "days", "enum", title: "Only on certain days of the week", multiple: true, required: false,
				options: ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]

			input "modes", "mode", title: "Only when mode is", multiple: true, required: false
		}
	}
}

private hideOptionsSection() {
	(starting || ending || days || modes) ? false : true
}

private String defaultLabel() {
	def lightsLabel = settings.lights.size() == 1 ? lights[0].displayName : lights[0].displayName + ", etc..."
	def triggerLabel = trigger.toLowerCase()

	switch(trigger) {
		case "Motion":
			triggerLabel = motionState == "active" ? "when motion starts" : "when motions stops"
			break
		case "Open/Close":
			triggerLabel = contactState == "open" ? "when ${deviceList(contactSensors)} opens" : "when ${deviceList(contactSensors)} closes"
			break
		case "Presence":
			triggerLabel = presenceState == "present" ? "when someone arrives" : "when everyone leaves"
			break
		case "Switch":
			triggerLabel = "when ${switchTrigger} turned ${switchState}"
			break
		case "Button":
			triggerLabel = "when ${buttonTrigger} pressed"
			break
		case "At a Specific Time":
			triggerLabel = "at ${hhmm(scheduledTime)}"
            if (scheduledTimeFollowup) {
            	triggerLabel += " and ${action == 'off' ? 'on' : 'off'} at ${hhmm(scheduledTimeFollowup)}"
            }
			break
		case "When Mode Changes":
			triggerLabel = "when mode changes to ${triggerModes.join(',','or')}"
			break
	}

	if (action == "color") {
		"Turn on and set color of $lightsLabel $triggerLabel"
	}
	else if (action == "level") {
		"Turn on and set level of $lightsLabel $triggerLabel"
	}
	else {
		if (trigger == "Button" && buttonToggle) {
			"Turn $lightsLabel on & off $triggerLabel"
		}
		else {
			"Turn $action $lightsLabel $triggerLabel"
		}
	}
}

private setColor() {

	def hueColor = 0
	def saturation = 100

	switch(color) {
		case "White":
			hueColor = 52
			saturation = 19
			break;
		case "Daylight":
			hueColor = 53
			saturation = 91
			break;
		case "Soft White":
			hueColor = 23
			saturation = 56
			break;
		case "Warm White":
			hueColor = 20
			saturation = 80
			break;
		case "Blue":
			hueColor = 70
			break;
		case "Green":
			hueColor = 39
			break;
		case "Yellow":
			hueColor = 25
			break;
		case "Orange":
			hueColor = 10
			break;
		case "Purple":
			hueColor = 75
			break;
		case "Pink":
			hueColor = 83
			break;
		case "Red":
			hueColor = 100
			break;
	}

	def value = [switch: "on", hue: hueColor, saturation: saturation, level: level as Integer ?: 100]
	log.debug "color = $value"

	lights.each {
		if (it.hasCapability("Color Control")) {
			it.setColor(value)
		}
		else if (it.hasCapability("Switch Level")) {
			it.setLevel(level as Integer ?: 100)
		}
		else {
			it.on()
		}
	}
}


private deviceList(device) {
	device.displayName
}

private deviceList(Collection devices) {
	devices.join(",", "or")
}

// TODO - centralize somehow
private getAllOk() {
	modeOk && daysOk && timeOk
}

private getModeOk() {
	def result = !modes || modes.contains(location.mode)
	log.trace "modeOk = $result"
	result
}

private getDaysOk() {
	def result = true
	if (days) {
		def df = new java.text.SimpleDateFormat("EEEE")
		if (location.timeZone) {
			df.setTimeZone(location.timeZone)
		}
		else {
			df.setTimeZone(TimeZone.getTimeZone("America/New_York"))
		}
		def day = df.format(new Date())
		result = days.contains(day)
	}
	log.trace "daysOk = $result"
	result
}

private getTimeOk() {
	def result = true
	if (starting && ending) {
		def currTime = now()
		def start = timeToday(starting).time
		def stop = timeToday(ending).time
		result = start < stop ? currTime >= start && currTime <= stop : currTime <= stop || currTime >= start
	}
	log.trace "timeOk = $result"
	result
}

private hhmm(time, fmt = "h:mm a")
{
	def t = timeToday(time, location.timeZone)
	def f = new java.text.SimpleDateFormat(fmt)
	f.setTimeZone(location.timeZone ?: timeZone(time))
	f.format(t)
}

private timeIntervalLabel()
{
	(starting && ending) ? hhmm(starting) + "-" + hhmm(ending, "h:mm a z") : ""
}

