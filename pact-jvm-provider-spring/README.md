# Pact Spring/JUnit runner

## Overview
Library provides ability to play contract tests against a provider using Spring & JUnit.
This library is based on and references the JUnit package, so see [junit provider support](pact-jvm-provider-junit) for more details regarding configuration using JUnit.

Supports:

- Standard ways to load pacts from folders and broker

- Easy way to change assertion strategy

- Spring Test MockMVC Controllers and ControllerAdvice using MockMvc standalonesetup.

- MockMvc debugger output

- Multiple @State runs to test a particular Provider State multiple times

- **au.com.dius.pact.provider.junit.State** custom annotation - before each interaction that requires a state change,
all methods annotated by `@State` with appropriate the state listed will be invoked.

## Example of MockMvc test

```java
    @RunWith(RestPactRunner.class) // Custom pact runner, child of PactRunner which runs only REST tests
    @Provider("myAwesomeService") // Set up name of tested provider
    @PactFolder("pacts") // Point where to find pacts (See also section Pacts source in documentation)
    public class ContractTest {
        //Create an instance of your controller.  We cannot autowire this as we're not using (and don't want to use)  a Spring test runner.
        @InjectMocks
        private AwesomeController awesomeController = new AwesomeController();

        //Mock your service logic class.  We'll use this to create scenarios for respective provider states.
        @Mock
        private AwesomeBusinessLogic awesomeBusinessLogic;

        //Create an instance of your controller advice (if you have one).  This will be passed to the MockMvcTarget constructor to be wired up with MockMvc.
        @InjectMocks
        private AwesomeControllerAdvice awesomeControllerAdvice = new AwesomeControllerAdvice();

        //Create a new instance of the MockMvcTarget and annotate it as the TestTarget for PactRunner
        @TestTarget
        public final MockMvcTarget target = new MockMvcTarget();

        @Before //Method will be run before each test of interaction
        public void before() {
            //initialize your mocks using your mocking framework
            MockitoAnnotations.initMocks(this);

            //configure the MockMvcTarget with your controller and controller advice
            target.setControllers(awesomeController);
            target.setControllerAdvice(awesomeControllerAdvice);
        }

        @State("default", "no-data") // Method will be run before testing interactions that require "default" or "no-data" state
        public void toDefaultState() {
            target.setRunTimes(3);  //let's loop through this state a few times for a 3 data variants
            when(awesomeBusinessLogic.getById(any(UUID.class)))
                .thenReturn(myTestHelper.generateRandomReturnData(UUID.randomUUID(), ExampleEnum.ONE))
                .thenReturn(myTestHelper.generateRandomReturnData(UUID.randomUUID(), ExampleEnum.TWO))
                .thenReturn(myTestHelper.generateRandomReturnData(UUID.randomUUID(), ExampleEnum.THREE));
        }

        @State("error-case")
        public void SingleUploadExistsState_Success() {
            target.setRunTimes(1); //tell the runner to only loop one time for this state
            
            //you might want to throw exceptions to be picked off by your controller advice
            when(awesomeBusinessLogic.getById(any(UUID.class)))
                .then(i -> { throw new NotCoolException(i.getArgumentAt(0, UUID.class).toString()); });
        }
    }
```
