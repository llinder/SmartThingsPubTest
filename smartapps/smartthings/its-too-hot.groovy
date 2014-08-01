/**
 *  It's Too Hot
 *
 *  Author: SmartThings
 */
definition(
    name: "It's Too Hot",
    namespace: "smartthings",
    author: "SmartThings",
    description: "Monitor the temperature and when it rises above your setting get a notification and/or turn on an A/C unit or fan.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/its-too-hot.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/its-too-hot@2x.png"
)

preferences {
	section("Monitor the temperature...") {
		input "temperatureSensor1", "capability.temperatureMeasurement"
	}
	section("When the temperature rises above...") {
		input "temperature1", "number", title: "Temperature?"
	}
    section( "Notifications" ) {
        input "sendPushMessage", "enum", title: "Send a push notification?", metadata:[values:["Yes","No"]], required:false
        input "phone1", "phone", title: "Send a Text Message?", required: false
    }
	section("Turn on which A/C or fan...") {
		input "switch1", "capability.switch", required: false
	}
}

def installed() {
	subscribe(temperatureSensor1, "temperature", temperatureHandler)
}

def updated() {
	unsubscribe()
	subscribe(temperatureSensor1, "temperature", temperatureHandler)
}

def temperatureHandler(evt) {
	log.trace "temperature: $evt.value, $evt"

	def tooHot = temperature1
	def mySwitch = settings.switch1

	// TODO: Replace event checks with internal state (the most reliable way to know if an SMS has been sent recently or not).
	if (evt.doubleValue <= tooHot) {
		log.debug "Checking how long the temperature sensor has been reporting <= $tooHot"

		// Don't send a continuous stream of text messages
		def deltaMinutes = 10 // TODO: Ask for "retry interval" in prefs?
		def timeAgo = new Date(now() - (1000 * 60 * deltaMinutes).toLong())
		def recentEvents = temperatureSensor1.eventsSince(timeAgo)
		log.trace "Found ${recentEvents?.size() ?: 0} events in the last $deltaMinutes minutes"
		def alreadySentSms = recentEvents.count { it.doubleValue <= tooHot } > 1

		if (alreadySentSms) {
			log.debug "SMS already sent to $phone1 within the last $deltaMinutes minutes"
			// TODO: Send "Temperature back to normal" SMS, turn switch off
		} else {
			log.debug "Temperature rose above $tooHot:  sending SMS to $phone1 and activating $mySwitch"
			send("${temperatureSensor1.label} is too hot, reporting a temperature of ${evt.value}${evt.unit?:"F"}")
			switch1?.on()
		}
	}
}

private send(msg) {
    if ( sendPushMessage != "No" ) {
        log.debug( "sending push message" )
        sendPush( msg )
    }

    if ( phone1 ) {
        log.debug( "sending text message" )
        sendSms( phone1, msg )
    }

    log.debug msg
}
