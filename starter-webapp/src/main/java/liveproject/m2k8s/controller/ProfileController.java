package liveproject.m2k8s.controller;

import liveproject.m2k8s.domain.Profile;
import liveproject.m2k8s.data.ProfileRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.ConstraintViolationException;
import javax.validation.Valid;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@RestController
@RequestMapping("/profile")
@Validated
public class ProfileController {

    private ProfileRepository profileRepository;

    @Value("${images.directory:/tmp}")
    private String uploadFolder;

    @Value("classpath:ghost.jpg")
    private Resource defaultImage;

    @Autowired
    public ProfileController(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }


    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ResponseEntity<String> handleConstraintViolationException(ConstraintViolationException e) {
        return new ResponseEntity<>("not valid due to validation error: " + e.getMessage(), HttpStatus.BAD_REQUEST);
    }
/*
| HTTP Method | URL                       | Behavior                            |
| ----------- | ------------------------- | ----------------------------------- |
| GET         | /profile/{username}       | get profile info for {username}     |
| POST        | /profile/{username}       | create profile info for {username}  |
| PUT         | /profile/{username}       | update profile info for {username}  |
| GET         | /profile/{username}/image | get profile image for {username}    |
| POST        | /profile/{username}/image | upload profile image for {username} |

 */
  /*  @RequestMapping(value = "/register", method = GET)
    public String showRegistrationForm(Model model) {
        model.addAttribute(new Profile());
        return "registerForm";
    }*/

    @PostMapping()
    @Transactional
    public ResponseEntity<Profile> createProfile(@Valid @RequestBody Profile profile) {
        log.info("Post Request: Create Profile {}", profile);
        Profile profileSaved = profileRepository.save(profile);

        return new ResponseEntity<>(profileSaved, HttpStatus.OK);
    }


    //@RequestMapping(value = "/{username}", method = GET)
    @GetMapping("/{username}")
    public ResponseEntity<Profile> showProfile(@PathVariable String username) {
        log.info("Get Request - Profile for: "+username);
        Profile profile = profileRepository.findByUsername(username);
        if (profile == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } else {
            return new ResponseEntity<>(profile,HttpStatus.OK);
        }

    }

//    @RequestMapping(value = "/{username}", method = POST)
    @Transactional
    @PutMapping("/{username}")
    public ResponseEntity<Profile> updateProfile(@PathVariable String username, @Valid @RequestBody Profile profile) {
        log.info("Updating model for: "+username);
        Profile dbProfile = profileRepository.findByUsername(username);
        boolean dirty = false;
        if (!StringUtils.isEmpty(profile.getEmail())
                && !profile.getEmail().equals(dbProfile.getEmail())) {
            dbProfile.setEmail(profile.getEmail());
            dirty = true;
        }
        if (!StringUtils.isEmpty(profile.getFirstName())
                && !profile.getFirstName().equals(dbProfile.getFirstName())) {
            dbProfile.setFirstName(profile.getFirstName());
            dirty = true;
        }
        if (!StringUtils.isEmpty(profile.getLastName())
                && !profile.getLastName().equals(dbProfile.getLastName())) {
            dbProfile.setLastName(profile.getLastName());
            dirty = true;
        }
        if (dirty) {
            profileRepository.save(dbProfile);
        }
        return new ResponseEntity<>(profile,HttpStatus.OK);
    }

     /* @RequestMapping(value = "/{username}/image.jpg", method = GET, produces = MediaType.IMAGE_JPEG_VALUE)
    @ResponseBody
   */
    @GetMapping(value = "/{username}/image",produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> displayImage(@PathVariable String username) throws IOException {
        log.info("Reading image for: "+username);
        InputStream in = null;
        try {
            Profile profile = profileRepository.findByUsername(username);
            if ((profile == null) || StringUtils.isEmpty(profile.getImageFileName())) {
                in = defaultImage.getInputStream();
            } else {
                in = new FileInputStream(profile.getImageFileName());
            }
            return new ResponseEntity<>(IOUtils.toByteArray(in),HttpStatus.OK);
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    @PostMapping(value = "/{username}/image")
    @Transactional
    public ResponseEntity uploadImage(@PathVariable String username, @RequestParam("file") MultipartFile file){
        log.info("Updating image for: "+username);
        if (file.isEmpty()) {
            return new ResponseEntity(HttpStatus.BAD_REQUEST);
            /*redirectAttributes.addFlashAttribute("imageMessage", "Empty file - please select a file to upload");
            return "redirect:/profile/" + username;*/
        }
        String fileName = file.getOriginalFilename();
        if (!(fileName.endsWith("jpg") || fileName.endsWith("JPG"))) {
            return new ResponseEntity(HttpStatus.BAD_REQUEST);
/*
            redirectAttributes.addFlashAttribute("imageMessage", "JPG files only - please select a file to upload");
            return "redirect:/profile/" + username;
*/
        }
        try {
            final String contentType = file.getContentType();
            // Get the file and save it somewhere
            byte[] bytes = file.getBytes();
            Path path = Paths.get(uploadFolder, username+".jpg");
            Files.write(path, bytes);
            Profile profile = profileRepository.findByUsername(username);
            profile.setImageFileName(path.toString());
            profile.setImageFileContentType(contentType);
            profileRepository.save(profile);
            /*redirectAttributes.addFlashAttribute("imageMessage",
                    "You successfully uploaded '" + fileName + "'");*/
        } catch (IOException e) {
            e.printStackTrace();
        }
        //return "redirect:/profile/" + username;
        return new ResponseEntity(HttpStatus.OK);
    }

}
