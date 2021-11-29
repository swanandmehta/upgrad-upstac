package org.upgrad.upstac.testrequests;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.event.annotation.BeforeTestClass;
import org.springframework.web.server.ResponseStatusException;
import org.upgrad.upstac.testrequests.TestRequest;
import org.upgrad.upstac.testrequests.consultation.Consultation;
import org.upgrad.upstac.testrequests.consultation.ConsultationController;
import org.upgrad.upstac.testrequests.consultation.CreateConsultationRequest;
import org.upgrad.upstac.testrequests.consultation.DoctorSuggestion;
import org.upgrad.upstac.testrequests.lab.CreateLabResult;
import org.upgrad.upstac.testrequests.lab.TestStatus;
import org.upgrad.upstac.testrequests.RequestStatus;
import org.upgrad.upstac.testrequests.TestRequestQueryService;
import org.upgrad.upstac.users.User;
import org.upgrad.upstac.users.models.Gender;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
@Slf4j
class ConsultationControllerTest {


    @Autowired
    ConsultationController consultationController;


    @Autowired
    TestRequestQueryService testRequestQueryService;

    @Autowired
    TestRequestService testRequestService;

    @Autowired
    TestRequestUpdateService testRequestUpdateService;


    private static User user;
    private static User tester;

    @BeforeTestClass
    public static void setUpBeforeClass() throws Exception {
        user = createUser();
        tester = createUser();
    }


    @Test
    @WithUserDetails(value = "doctor")
    public void calling_assignForConsultation_with_valid_test_request_id_should_update_the_request_status(){

        CreateTestRequest createTestRequest = createTestRequest();

        TestRequest testRequest = testRequestService.createTestRequestFrom(user,createTestRequest);

        testRequestUpdateService.assignForLabTest(testRequest.requestId, tester);

        testRequestUpdateService.updateLabTest(testRequest.requestId, getCreateLabResult(testRequest), tester);

        testRequest = getTestRequestByStatus(RequestStatus.LAB_TEST_COMPLETED);

        TestRequest testRequest1 = consultationController.assignForConsultation(testRequest.requestId);

        assertNotNull(testRequest1);
        assertEquals(testRequest.requestId, testRequest1.requestId);
        assertEquals(testRequest1.getStatus(), RequestStatus.DIAGNOSIS_IN_PROCESS);
    }

    private static User createUser() {
        User user = new User();
        user.setUserName(UUID.randomUUID().toString());
        return user;
    }

    public CreateTestRequest createTestRequest() {
        Random random = new Random();
        CreateTestRequest createTestRequest = new CreateTestRequest();
        createTestRequest.setAddress("some Addres");
        createTestRequest.setAge(98);
        createTestRequest.setEmail("someone" + UUID.randomUUID().toString() + "@somedomain.com");
        createTestRequest.setGender(Gender.MALE);
        createTestRequest.setName("someuser");
        createTestRequest.setPhoneNumber(Integer.toString(ThreadLocalRandom.current().nextInt(100000, 999998 + 1)));
        createTestRequest.setPinCode(716768);
        return createTestRequest;
    }

    public CreateLabResult getCreateLabResult(TestRequest testRequest) {

        //Create an object of CreateLabResult and set all the values
        // Return the object
        CreateLabResult result = new CreateLabResult();
        result.setBloodPressure("N/A");
        result.setHeartBeat("N/A");
        result.setTemperature("N/A");
        result.setOxygenLevel("N/A");
        result.setComments("N/A");
        result.setResult(TestStatus.NEGATIVE);

        return result;
    }

    public TestRequest getTestRequestByStatus(RequestStatus status) {
        return testRequestQueryService.findBy(status).stream().findFirst().get();
    }

    @Test
    @WithUserDetails(value = "doctor")
    public void calling_assignForConsultation_with_valid_test_request_id_should_throw_exception(){

        Long InvalidRequestId= -34L;

        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> {
            consultationController.assignForConsultation(InvalidRequestId);
        });

        assertThat("Invalid Id", error.getMessage().contains("Invalid ID"));
    }

    @Test
    @WithUserDetails(value = "doctor")
    public void calling_updateConsultation_with_valid_test_request_id_should_update_the_request_status_and_update_consultation_details(){

        CreateTestRequest createTestRequest = createTestRequest();

        TestRequest testRequest = testRequestService.createTestRequestFrom(user,createTestRequest);

        testRequestUpdateService.assignForLabTest(testRequest.requestId, tester);

        testRequestUpdateService.updateLabTest(testRequest.requestId, getCreateLabResult(testRequest), tester);

        consultationController.assignForConsultation(testRequest.requestId);

        testRequest = getTestRequestByStatus(RequestStatus.DIAGNOSIS_IN_PROCESS);

        TestRequest testRequest1 = consultationController.updateConsultation(testRequest.requestId, getCreateConsultationRequest(testRequest));
        testRequest = testRequestQueryService.getTestRequestById(testRequest1.requestId).get();

        assertEquals(testRequest.getStatus(), RequestStatus.COMPLETED);
        assertEquals(testRequest1.requestId, testRequest.requestId);
        assertEquals(testRequest1.getConsultation().getSuggestion(), testRequest.getConsultation().getSuggestion());
    }


    @Test
    @WithUserDetails(value = "doctor")
    public void calling_updateConsultation_with_invalid_test_request_id_should_throw_exception(){

        CreateTestRequest createTestRequest = createTestRequest();

        TestRequest testRequest = testRequestService.createTestRequestFrom(user,createTestRequest);

        testRequestUpdateService.assignForLabTest(testRequest.requestId, tester);

        testRequestUpdateService.updateLabTest(testRequest.requestId, getCreateLabResult(testRequest), tester);

        consultationController.assignForConsultation(testRequest.requestId);

        ResponseStatusException err = assertThrows(ResponseStatusException.class, () -> {
            TestRequest testRequest1 = getTestRequestByStatus(RequestStatus.DIAGNOSIS_IN_PROCESS);
            consultationController.updateConsultation(-1L, getCreateConsultationRequest(testRequest1));
        });

        assertThat("Invalid ID", err.getMessage().contains("Invalid ID"));

    }

    @Test
    @WithUserDetails(value = "doctor")
    public void calling_updateConsultation_with_invalid_empty_status_should_throw_exception(){

        CreateTestRequest createTestRequest = createTestRequest();

        TestRequest testRequest = testRequestService.createTestRequestFrom(user,createTestRequest);

        testRequestUpdateService.assignForLabTest(testRequest.requestId, tester);

        testRequestUpdateService.updateLabTest(testRequest.requestId, getCreateLabResult(testRequest), tester);

        consultationController.assignForConsultation(testRequest.requestId);

        testRequest = getTestRequestByStatus(RequestStatus.DIAGNOSIS_IN_PROCESS);

        ResponseStatusException err = assertThrows(ResponseStatusException.class, () -> {
            TestRequest testRequest1 = getTestRequestByStatus(RequestStatus.DIAGNOSIS_IN_PROCESS);
            CreateConsultationRequest request = getCreateConsultationRequest(testRequest1);
            request.setSuggestion(null);
            consultationController.updateConsultation(testRequest1.requestId, request);
        });
    }

    public CreateConsultationRequest getCreateConsultationRequest(TestRequest testRequest) {

        CreateConsultationRequest result = new CreateConsultationRequest();

        if (testRequest.getLabResult().getResult() == TestStatus.POSITIVE) {
            result.setSuggestion(DoctorSuggestion.HOME_QUARANTINE);
        } else {
            result.setSuggestion(DoctorSuggestion.NO_ISSUES);
        }

        return result;

    }

}