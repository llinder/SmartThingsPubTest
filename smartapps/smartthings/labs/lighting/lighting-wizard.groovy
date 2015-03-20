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
	namespace: "smartthings/labs/lighting",
	author: "SmartThings",
	description: "Controls your lights and switches based on a variety of inputs including motion, open/close, presence, mode changes, specific times, sunrise/sunset, and more.",
	category: "SmartThings Labs",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/ModeMagic/Cat-ModeMagic.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/ModeMagic/Cat-ModeMagic@2x.png",
	iconX3Url: "https://s3.amazonaws.com/smartapp-icons/ModeMagic/Cat-ModeMagic@3x.png",
)


preferences {
	page(name: "mainPage", title: "Automations", install: true, uninstall: true) {
		section {
			app(name: "automations", appName: "Lighting Automation", namespace: "smartthings/labs/lighting", title: "New Automation", multiple: true)
		}
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"
}

def updated() {
	log.debug "Updated with settings: ${settings}"
}