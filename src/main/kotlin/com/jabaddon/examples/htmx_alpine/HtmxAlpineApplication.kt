package com.jabaddon.examples.htmx_alpine

import jakarta.servlet.http.HttpServletResponse
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.ModelAndView
import java.util.*

@SpringBootApplication
class HtmxAlpineApplication

fun main(args: Array<String>) {
	runApplication<HtmxAlpineApplication>(*args)
}

data class Contact(val id: UUID, val name: String, val email: String)

@Component
class ContactRepository {
	private val contacts = mutableListOf<Contact>()

	init {
		contacts.add(Contact(UUID.randomUUID(), "John Snow", "john.snow@got.com"))
		contacts.add(Contact(UUID.randomUUID(), "Arya Stark", "arya.stark@got.com"))
	}

	fun addContact(contact: Contact) {
		contacts.add(contact)
	}

	fun getContacts(): List<Contact> {
		return contacts
	}

	fun deleteContact(id: UUID) {
		contacts.removeIf { it.id == id }
	}
}

@RestController
@RequestMapping("/api")
class ContactResourceController(private val contactRepository: ContactRepository) {

	@PostMapping("/contacts")
	fun newContact(@RequestParam name: String,
				   @RequestParam email: String,
				   httpServletResponse: HttpServletResponse
	): Map<String, String> {
		if (!email.endsWith("@got.com")) {
			throw IllegalArgumentException("Email must end with @got.com")
		}
		val contact = Contact(UUID.randomUUID(), name, email)
		contactRepository.addContact(contact)
		httpServletResponse.addHeader("HX-Trigger", "contacts-table-refresh")
		return mapOf("message" to "Contact added")
	}

	@DeleteMapping("/contacts/{id}")
	fun deleteContact(@PathVariable id: UUID, httpServletResponse: HttpServletResponse): Map<String, String> {
		contactRepository.deleteContact(id)
		httpServletResponse.addHeader("HX-Trigger", "contacts-table-refresh")
		return mapOf("message" to "Contact with id $id deleted")
	}

	@ExceptionHandler(IllegalArgumentException::class)
	fun handleIllegalArgumentException(ex: IllegalArgumentException): ResponseEntity<Map<String, String>> {
		val response = mapOf("error" to ex.message.orEmpty())
		return ResponseEntity(response, HttpStatus.BAD_REQUEST)
	}
}

@Controller
class ContactController(private val contactRepository: ContactRepository) {
	@GetMapping("/")
	fun index(): ModelAndView {
		return ModelAndView("index", mapOf("contacts" to contactRepository.getContacts()))
	}

	@GetMapping("/contacts")
	fun contacts(): ModelAndView {
		return ModelAndView("contacts_table", mapOf("contacts" to contactRepository.getContacts()))
	}
}

