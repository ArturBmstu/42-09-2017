package ru.mail.park.controllers;

import com.fasterxml.jackson.annotation.JsonView;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.mail.park.controllers.messages.MessageConstants;
import ru.mail.park.controllers.messages.UserMessage;
import ru.mail.park.controllers.validators.Validator;
import ru.mail.park.view.View;
import ru.mail.park.info.UserUpdateInfo;
import ru.mail.park.models.User;
import ru.mail.park.info.UserSigninInfo;
import ru.mail.park.controllers.messages.Message;
import ru.mail.park.services.UserService;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import java.util.ArrayList;
import java.util.List;

@CrossOrigin(origins = { "https://sand42box.herokuapp.com",
        "https://nightly-42.herokuapp.com",
        "https://master-42.herokuapp.com" }
        )
@RestController
@RequestMapping(path = "/api/auth")
public class UserController {
    private final UserService userService;
    private final Validator validator;

    private static final String SESSION_ATTR = "user_info";

    public UserController(UserService userService, Validator validator) {
        this.userService = userService;
        this.validator = validator;
    }

    @PostMapping("signup")
    public ResponseEntity<Message<?>> signup(@Valid @RequestBody User userSignupInfo) {
        userService.addUser(userSignupInfo);
        return ResponseEntity.ok(new Message<>(MessageConstants.SIGNED_UP));
    }

    @PutMapping("update")
    @JsonView(View.SummaryWithMessage.class)
    public ResponseEntity<? extends Message<?>> update(@RequestBody UserUpdateInfo userUpdateInfo, HttpSession httpSession) {
        String validateResult;
        List<String> responseList = new ArrayList<>();

        String username = (String) httpSession.getAttribute(SESSION_ATTR);
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new Message<>(MessageConstants.UNAUTHORIZED));
        }

        if (userUpdateInfo.getUsername() != null) {
            validateResult = validator.validateUsername(userUpdateInfo.getUsername());
            if (validateResult != null) {
                responseList.add(validateResult);
            }
        }
        if (userUpdateInfo.getEmail() != null) {
            validateResult = validator.validateEmail(userUpdateInfo.getEmail());
            if (validateResult != null) {
                responseList.add(validateResult);
            }
        }
        if (userUpdateInfo.getOldPassword() != null) {
            final String oldPassword = userUpdateInfo.getOldPassword();
            if (userService.checkUserAndPassword(username, oldPassword) == null) {
                responseList.add(MessageConstants.BAD_OLD_PASSWORD);
            }
            if (userUpdateInfo.getPassword() != null) {
                final String newPassword = userUpdateInfo.getPassword();
                validateResult = Validator.validatePassword(newPassword);
                if (validateResult != null) {
                    responseList.add(validateResult);
                }
            } else {
                responseList.add(MessageConstants.EMPTY_PASSWORD);
            }
        } else if (userUpdateInfo.getPassword() != null) {
            responseList.add(MessageConstants.EMPTY_OLD_PASSWORD);
        }

        if (!responseList.isEmpty()) {
            return ResponseEntity.badRequest().body(new Message<>(responseList));
        }

        User user = userService.updateUser(username, userUpdateInfo);
        if (userUpdateInfo.getUsername() != null) {
            httpSession.setAttribute(SESSION_ATTR, userUpdateInfo.getUsername());
        }
        return ResponseEntity.ok(new UserMessage<>(MessageConstants.UPDATED, user));
    }

    @GetMapping("me")
    @JsonView(View.Summary.class)
    public ResponseEntity<?> me(HttpSession httpSession) {
        String username = (String) httpSession.getAttribute(SESSION_ATTR);
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        return ResponseEntity.ok(new UserMessage<>(null, userService.getByUsername(username)));
    }

    @PostMapping("login")
    public ResponseEntity<Message<?>> login(@RequestBody UserSigninInfo userSigninInfo, HttpSession httpSession) {
        List<String> responseList = new ArrayList<>();

        if (userSigninInfo.getLogin() == null || userSigninInfo.getLogin().isEmpty()) {
            responseList.add(MessageConstants.EMPTY_USERNAME);
        }
        if (userSigninInfo.getPassword() == null || userSigninInfo.getPassword().isEmpty()) {
            responseList.add(MessageConstants.EMPTY_PASSWORD);
        }

        if (!responseList.isEmpty()) {
            return ResponseEntity.badRequest().body(new Message<>(responseList));
        }

        final User user = userService.checkUserAndPassword(userSigninInfo.getLogin(), userSigninInfo.getPassword());
        if (user == null) {
            responseList.add(MessageConstants.BAD_LOGIN_DATA);
            return ResponseEntity.badRequest().body(new Message<>(responseList));
        }
        httpSession.setAttribute(SESSION_ATTR, user.getUsername());
        responseList.add(MessageConstants.LOGGED_IN);
        return ResponseEntity.ok(new Message<>(responseList));
    }

    @DeleteMapping("logout")
    public ResponseEntity<Message<String>> logout(HttpSession httpSession) {
        if (httpSession.getAttribute(SESSION_ATTR) == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new Message<>(MessageConstants.UNAUTHORIZED));
        }
        httpSession.invalidate();
        return ResponseEntity.ok(new Message<>(MessageConstants.LOGGED_OUT));
    }

}


