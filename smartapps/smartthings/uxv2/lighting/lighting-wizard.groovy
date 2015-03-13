/**
 *  Lighting Wizard
 *
 *  Copyright 2015 SmartThings
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
	name: "Lighting Wizard",
	namespace: "smartthings/uxv2/lighting",
	author: "SmartThings",
	description: "Prototype lighting automation app uses dynamically updating preferences page to configure actions and triggers. Also automatically names app.",
	category: "SmartSolutions",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/ModeMagic/Cat-ModeMagic.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/ModeMagic/Cat-ModeMagic@2x.png",
	iconX3Url: "https://s3.amazonaws.com/smartapp-icons/ModeMagic/Cat-ModeMagic@3x.png",
)


preferences {
	page(name: "mainPage", title: "Automations", install: true, uninstall: true) {
		section {
			app(name: "automations", appName: "Lighting Automation", namespace: "smartthings/uxv2/lighting", title: "New Automation", multiple: true)
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
	initialize()
}

def initialize() {
	updateSolutionSummary()
}

cards {
	if (state?.configured) {
		card("Recently") {
			tiles {
				eventTile {}
			}
		}
	}

	card(name: "Lighting Wizard", type: "html", action: "home") {}
}

mappings {
	path("/home") {
		action: [
			GET: "home"
		]
	}
}

def updateSolutionSummary() {
	def count = childApps?.size() ?: 0
	def summaryData = []
	if (count) {
		summaryData << ["icon":"indicator-dot-green","iconColor":"#49a201","default":"true","value":"$count automations configured"]
		state.configured = true
	} else {
		summaryData << ["icon":"indicator-dot-gray","iconColor":"#878787","default":"true","value":"no automations configured"]
		state.configured = false
	}
	sendEvent(linkText:app.label, descriptionText:app.label + " updating summary", eventType:"SOLUTION_SUMMARY",
		name: "summary", value: summaryData*.value?.join(", "), data:summaryData, isStateChange: true, displayed: false)
}


def home() {
	renderHTML("Lighting Wizard") { html ->
		html.head {
			"""
        <link rel="stylesheet" href="https://s3.amazonaws.com/smartapp-resources/incidents/smartthings-cards.css" type="text/css" media="screen" />
        <style>
        h1{
            font-size:16px;
            margin: 10px;
		}
        p {
        	font-size: 14px;
            margin: 10px;
        }
        </style>
        """
		}
		html.body {
			"""
        <h1>Lighting Automation Prototype</h1>
        <p>
        This module houses a prototype SmartApp for creating automations of lights and switches based on a variety of input events such as motion,
        open/close, sunrise/sunset, etc. This one app covers all of these use cases by using a dynamically updating preferences page that displays only
        those controls that are applicable to the current automation. It covers all current <em>Lights & Switches</em> module use cases
        as well as additional functions such as the control of dimmers and color. To start creating automations, tap the gear icon and then tap
        <em>New Automation</em>.
        </p>
        <p>
        The dynamically updating preference page allows the options to be tailored according to the devices and actions selected. For example,
        if you select only on/off switches, there's no option to set the dimmer level, but if you select at least one dimmer, then this
        option is available. Similarly, an option to control bulb color is only presented if your device selection includes bulbs with the color control
        capability.
        </p>
        <p>When configuring an app, pay attention to the network utilization spinner at the top of the screen. This control
        provides the only indication that the dynamic preference page is updating. Sometimes it can take a while for the new controls
        to appear on the page after you've made a selection. We may add a more visible indication that the page is still updating in the future.
        </p>
        """
		}
	}
}