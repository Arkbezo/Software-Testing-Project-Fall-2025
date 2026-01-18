package IntegrationTesting;

import model.dto.CustomerDTO;
import model.dto.Response;
import model.dto.MessageType;
import model.validators.CommonValidator;

import org.junit.Test;

import static org.junit.Assert.*;

public class ValidationBottomUpIntegrationTest {

    @Test
    public void validateCustomer_nullPhone_generatesErrorMessage() {
        // Driver creates inputs (Bottom-Up uses driver to call low-level units)
        CustomerDTO c = new CustomerDTO();
        c.setName("TestCustomer");
        c.setPhoneNumber(null);

        Response res = new Response();

        // Integrate: PhoneValidation + CommonValidator + Response
        CommonValidator.validateObject(c, res);

        assertFalse(res.isSuccessfull());
        assertEquals(MessageType.Error, res.messagesList.get(0).type);
        assertTrue(res.getErrorMessages().toLowerCase().contains("phone"));
    }

    @Test
    public void validateCustomer_shortPhone_generatesErrorMessage() {
        CustomerDTO c = new CustomerDTO();
        c.setName("TestCustomer");
        c.setPhoneNumber("123456789"); // 9 chars

        Response res = new Response();

        CommonValidator.validateObject(c, res);

        assertFalse(res.isSuccessfull());
        assertEquals(MessageType.Error, res.messagesList.get(0).type);
        assertTrue(res.getErrorMessages().toLowerCase().contains("phone"));
    }

    @Test
    public void validateCustomer_validPhone_noError_success() {
        CustomerDTO c = new CustomerDTO();
        c.setName("TestCustomer");
        c.setPhoneNumber("1234567890"); // 10 chars

        Response res = new Response();

        CommonValidator.validateObject(c, res);

        assertTrue(res.isSuccessfull());
        assertEquals("", res.getErrorMessages()); // no error messages expected
    }
}
