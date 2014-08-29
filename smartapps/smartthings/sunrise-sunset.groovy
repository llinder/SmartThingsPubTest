/**
 *  Sunrise, Sunset
 *
 *  Author: SmartThings
 *
 *  Date: 2013-04-30
 */
definition(
    name: "Sunrise/Sunset",
    namespace: "smartthings",
    author: "SmartThings",
    description: "Changes mode and controls lights based on local sunrise and sunset times.",
    category: "Mode Magic",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/ModeMagic/rise-and-shine.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/ModeMagic/rise-and-shine@2x.png"
)

preferences {
	section ("At sunrise...") {
		input "sunriseMode", "mode", title: "Change mode to?", required: false
		input "sunriseOn", "capability.switch", title: "Turn on?", required: false, multiple: true
		input "sunriseOff", "capability.switch", title: "Turn off?", required: false, multiple: true
	}
	section ("At sunset...") {
		input "sunsetMode", "mode", title: "Change mode to?", required: false
		input "sunsetOn", "capability.switch", title: "Turn on?", required: false, multiple: true
		input "sunsetOff", "capability.switch", title: "Turn off?", required: false, multiple: true
	}
	section ("Sunrise offset (optional)...") {
		input "sunriseOffsetValue", "text", title: "HH:MM", required: false
		input "sunriseOffsetDir", "enum", title: "Before or After", required: false, metadata: [values: ["Before","After"]]
	}
	section ("Sunset offset (optional)...") {
		input "sunsetOffsetValue", "text", title: "HH:MM", required: false
		input "sunsetOffsetDir", "enum", title: "Before or After", required: false, metadata: [values: ["Before","After"]]
	}
	section ("Zip code (optional, defaults to location coordinates)...") {
		input "zipCode", "text", required: false
	}
	section( "Notifications" ) {
		input "sendPushMessage", "enum", title: "Send a push notification?", metadata:[values:["Yes", "No"]], required: false
		input "phoneNumber", "phone", title: "Send a text message?", required: false
	}

}

def installed() {
	initialize()
}

def updated() {
	unsubscribe()
	//unschedule handled in astroCheck method
	initialize()
}

def initialize() {
	subscribe(location, "position", locationPositionChange)
	subscribe(location, "sunriseTime", sunriseSunsetTimeHandler)
	subscribe(location, "sunsetTime", sunriseSunsetTimeHandler)
	
	astroCheck()
}

def locationPositionChange(evt) {
	log.trace "locationChange()"
	astroCheck()
}

def sunriseSunsetTimeHandler(evt) {
	log.trace "sunriseSunsetTimeHandler()"
	astroCheck()
}

def astroCheck() {
	def s = getSunriseAndSunset(zipCode: zipCode, sunriseOffset: sunriseOffset, sunsetOffset: sunsetOffset)

	def now = new Date()
	def riseTime = s.sunrise
	def setTime = s.sunset
	log.debug "riseTime: $riseTime"
	log.debug "setTime: $setTime"
	
	if (state.riseTime != riseTime.time) {
		state.riseTime = riseTime.time
		
		unschedule("sunriseHandler")
		if(riseTime.before(now)) {
			riseTime.next()
		}
		log.info "scheduling sunrise handler for $riseTime"
		runDaily(riseTime, sunriseHandler)
	}
   
	if (state.setTime != setTime.time) {
		state.setTime = setTime.time
		unschedule("sunsetHandler")

	    if(setTime.before(now)) {
	        setTime.next()
	    }
	    log.info "scheduling sunset handler for $setTime"
	    runDaily(setTime, sunsetHandler)
	}
}

def sunriseHandler() {
	log.info "Executing sunrise handler"
	if (sunriseOn) {
		sunriseOn.on()
	}
	if (sunriseOff) {
		sunriseOff.off()
	}
	changeMode(sunriseMode)
}

def sunsetHandler() {
	log.info "Executing sunset handler"
	if (sunsetOn) {
		sunsetOn.on()
	}
	if (sunsetOff) {
		sunsetOff.off()
	}
	changeMode(sunsetMode)
}

def changeMode(newMode) {
	if (newMode && location.mode != newMode) {
		if (location.modes?.find{it.name == newMode}) {
			setLocationMode(newMode)
			send "${label} has changed the mode to '${newMode}'"
		}
		else {
			send "${label} tried to change to undefined mode '${newMode}'"
		}
	}
}

private send(msg) {
	if ( sendPushMessage != "No" ) {
		log.debug( "sending push message" )
		sendPush( msg )
	}

	if ( phoneNumber ) {
		log.debug( "sending text message" )
		sendSms( phoneNumber, msg )
	}

	log.debug msg
}

private getLabel() {
	app.label ?: "SmartThings"
}

private getSunriseOffset() {
	sunriseOffsetValue ? (sunriseOffsetDir == "Before" ? "-$sunriseOffsetValue" : sunriseOffsetValue) : null
}

private getSunsetOffset() {
	sunsetOffsetValue ? (sunsetOffsetDir == "Before" ? "-$sunsetOffsetValue" : sunsetOffsetValue) : null
}

