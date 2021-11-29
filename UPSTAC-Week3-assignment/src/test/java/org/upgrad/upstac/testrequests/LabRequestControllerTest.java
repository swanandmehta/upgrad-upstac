package org.upgrad.upstac.testrequests;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.event.annotation.BeforeTestClass;
import org.upgrad.upstac.exception.AppException;
import org.upgrad.upstac.exception.UpgradResponseStatusException;
import org.upgrad.upstac.testrequests.lab.CreateLabResult;
import org.upgrad.upstac.testrequests.lab.LabRequestController;
import org.upgrad.upstac.testrequests.lab.TestStatus;
import org.upgrad.upstac.users.User;
import org.upgrad.upstac.users.models.Gender;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
@Slf4j
class LabRequestControllerTest {


    @Autowired
    LabRequestController labRequestController;


    @Autowired
    TestRequestQueryService testRequestQueryService;


    @Autowired
    TestRequestService testRequestService;

    private static User user;

    @BeforeTestClass
    public static void setUpBeforeClass() throws Exception {
        user = createUser();
    }

    @Test
    @WithUserDetails(value = "tester")
    public void calling_assignForLabTest_with_valid_test_request_id_should_update_the_request_status(){

        CreateTestRequest createTestRequest = createTestRequest();
        TestRequest testRequest = testRequestService.createTestRequestFrom(user,createTestRequest);

        TestRequest testRequest1 = labRequestController.assignForLabTest(testRequest.requestId);

        assertNotNull(testRequest1);
        assertEquals(testRequest.requestId, testRequest1.requestId);
        assertEquals(testRequest1.getStatus(), RequestStatus.LAB_TEST_IN_PROGRESS);
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

    public TestRequest getTestRequestByStatus(RequestStatus status) {
        return testRequestQueryService.findBy(status).stream().findFirst().get();
    }

    @Test
    @WithUserDetails(value = "tester")
    public void calling_assignForLabTest_with_valid_test_request_id_should_throw_exception(){

        Long InvalidRequestId= -34L;

        AppException thrown = assertThrows(AppException.class, () -> {
            labRequestController.assignForLabTest(InvalidRequestId);
        });

        assertThat("Invalid ID", thrown.getMessage().contains("Invalid ID"));

    }

    @Test
    @WithUserDetails(value = "tester")
    public void calling_updateLabTest_with_valid_test_request_id_should_update_the_request_status_and_update_test_request_details(){

        CreateTestRequest createTestRequest = createTestRequest();
        TestRequest testRequest = testRequestService.createTestRequestFrom(user,createTestRequest);

        testRequest = labRequestController.assignForLabTest(testRequest.requestId);

        TestRequest testRequest1 = labRequestController.updateLabTest(testRequest.requestId, getCreateLabResult(testRequest));

        assertEquals(testRequest.requestId, testRequest1.requestId);
        assertEquals(testRequest1.getStatus(), RequestStatus.LAB_TEST_COMPLETED);

    }


    @Test
    @WithUserDetails(value = "tester")
    public void calling_updateLabTest_with_invalid_test_request_id_should_throw_exception(){
        Long InvalidRequestId= -34L;

        UpgradResponseStatusException thrown = assertThrows(UpgradResponseStatusException.class, () -> {
            labRequestController.updateLabTest(InvalidRequestId, getCreateLabResult(null));
        });

        assertThat("Invalid ID", thrown.getMessage().contains("Invalid ID"));

    }

    @Test
    @WithUserDetails(value = "tester")
    public void calling_updateLabTest_with_invalid_empty_status_should_throw_exception(){
        NullPointerException thrown = assertThrows(NullPointerException.class, () -> {
            CreateTestRequest createTestRequest = createTestRequest();
            TestRequest testRequest = testRequestService.createTestRequestFrom(user,createTestRequest);
            testRequest = labRequestController.assignForLabTest(testRequest.requestId);
            labRequestController.updateLabTest(testRequest.requestId, null);
        });
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

}