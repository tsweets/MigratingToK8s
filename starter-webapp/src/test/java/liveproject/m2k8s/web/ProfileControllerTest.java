package liveproject.m2k8s.web;

import liveproject.m2k8s.domain.Profile;
import liveproject.m2k8s.controller.ProfileController;
import liveproject.m2k8s.data.ProfileRepository;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@Ignore
public class ProfileControllerTest {

  @Test
  public void shouldShowRegistration() throws Exception {
    MockMvc mockMvc = standaloneSetup(buildProfileController()).build();
    mockMvc.perform(get("/profile/register"))
           .andExpect(view().name("registerForm"));
  }
  
  @Test
  public void shouldProcessRegistration() throws Exception {
    ProfileRepository mockRepository = mock(ProfileRepository.class);
    Profile unsaved = new Profile("jbauer", "24hours", "Jack", "Bauer", "jbauer@ctu.gov");
    Profile saved = new Profile(24L, "jbauer", "24hours", "Jack", "Bauer", "jbauer@ctu.gov");
    when(mockRepository.save(unsaved)).thenReturn(saved);

    ProfileController controller = new ProfileController(mockRepository);
    MockMvc mockMvc = standaloneSetup(controller).build();

    mockMvc.perform(post("/profile/register")
           .param("firstName", "Jack")
           .param("lastName", "Bauer")
           .param("username", "jbauer")
           .param("password", "24hours")
           .param("email", "jbauer@ctu.gov"))
           .andExpect(redirectedUrl("/profile/jbauer"));
    
    verify(mockRepository, atLeastOnce()).save(unsaved);
  }

  @Test
  public void shouldFailValidationWithNoData() throws Exception {
    MockMvc mockMvc = standaloneSetup(buildProfileController()).build();
    
    mockMvc.perform(post("/profile/register"))
        .andExpect(status().isOk())
        .andExpect(view().name("registerForm"))
        .andExpect(model().errorCount(5))
        .andExpect(model().attributeHasFieldErrors(
            "profile", "firstName", "lastName", "username", "password", "email"));
  }

  private ProfileController buildProfileController() {
    ProfileRepository mockRepository = mock(ProfileRepository.class);
    ProfileController controller = new ProfileController(mockRepository);
    return controller;
  }
}
