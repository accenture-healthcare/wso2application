package com.wso2persistence.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.wso2persistence.service.PersistenceService;

@RestController
public class PersistenceController {

	@Autowired
	private PersistenceService persistenceService;

	@PostMapping("/delete")
	public ResponseEntity<Object> deleteXls(@RequestParam("function") String function,
			@RequestParam("enteSanitario") String enteSanitario, @RequestParam("ambito") String ambito,
			@RequestParam(required = false) String messageType,
			@RequestParam(required = false, name = "version") Integer version) {
		try {
			persistenceService.delete(function, enteSanitario, ambito, messageType, version);
			return new ResponseEntity<>(HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PostMapping("/update")
	public ResponseEntity<Object> uploadXls(@RequestParam("file") MultipartFile file,
			@RequestParam("function") String function, @RequestParam("enteSanitario") String enteSanitario,
			@RequestParam("ambito") String ambito, @RequestParam(required = false) String messageType,
			@RequestParam(required = true, name = "version") Integer version) {
		try {
			return new ResponseEntity<>(
					persistenceService.upload(function, file, enteSanitario, ambito, messageType, version),
					HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

}
