package IntegrationTesting;

import dal.DALManager;
import model.dto.EmployeeDTO;
import model.dto.Response;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class EmployeeCRUDIntegrationTest {

    private DALManager dalManager;

    private EmployeeDTO employee;     // used for update + delete
    private int employeeId;           // synced from DB list

    private String uniqueName;
    private String initialPhone;
    private String updatedPhone;

    @BeforeEach
    void setUp() {
        dalManager = new DALManager();

        String uid = UUID.randomUUID().toString().substring(0, 6);
        uniqueName = "IntEmp_" + uid;
        initialPhone = "0670000000";  // 10 digits
        updatedPhone = "0671111111";  // 10 digits

        // 1) INSERT (DAL)
        employee = new EmployeeDTO();
        employee.setName(uniqueName);
        employee.setPhoneNumber(initialPhone);

        Response resInsert = new Response();
        dalManager.saveEmployee(employee, resInsert);
        assertTrue(resInsert.isSuccessfull(), "Employee insert failed: " + resInsert.getErrorMessages());

        // 2) SYNC ID (DAL read)
        employeeId = syncEmployeeIdByName(uniqueName);
        assertTrue(employeeId > 0, "Synced employee id must be > 0");

        // Set ID so update/delete target the correct row
        employee.setId(employeeId);
    }

    @AfterEach
    void tearDown() {
        if (employee != null && employee.getId() > 0) {
            Response resDelete = new Response();
            dalManager.deleteEmployee(employee, resDelete);
            assertTrue(resDelete.isSuccessfull(), "Employee delete failed: " + resDelete.getErrorMessages());
        }
    }

    @Test
    void insert_update_read_verify_delete_employee_complex_flow() {

        // 3) UPDATE (DAL)
        String updatedName = uniqueName + "_UPD";
        employee.setName(updatedName);
        employee.setPhoneNumber(updatedPhone);

        Response resUpdate = new Response();
        dalManager.updateEmployee(employee, resUpdate);
        assertTrue(resUpdate.isSuccessfull(), "Employee update failed: " + resUpdate.getErrorMessages());

        // 4) READ (DAL)
        Response resRead = new Response();
        ArrayList<EmployeeDTO> employees = dalManager.getEmployees(resRead);

        assertTrue(resRead.isSuccessfull(), "Get employees failed: " + resRead.getErrorMessages());
        assertNotNull(employees);
        assertFalse(employees.isEmpty(), "Employees list should not be empty");

        // 5) VERIFY (find by id)
        EmployeeDTO found = employees.stream()
                .filter(e -> e.getId() == employeeId)
                .findFirst()
                .orElse(null);

        assertNotNull(found, "Updated employee not found in getEmployees()");

        assertEquals(updatedName, found.getName(), "Employee name was not updated correctly");
        assertEquals(updatedPhone, found.getPhoneNumber(), "Employee phoneNumber was not updated correctly");

        // verify old name is not present for this id
        assertNotEquals(uniqueName, found.getName(), "Employee name should not still be old value");
    }

    private int syncEmployeeIdByName(String name) {
        Response res = new Response();
        ArrayList<EmployeeDTO> employees = dalManager.getEmployees(res);

        assertTrue(res.isSuccessfull(), "Get employees failed while syncing ID: " + res.getErrorMessages());
        assertNotNull(employees);

        EmployeeDTO found = employees.stream()
                .filter(e -> name.equals(e.getName()))
                .findFirst()
                .orElse(null);

        assertNotNull(found, "Could not sync inserted employee ID from DB (employee not found by name)");
        return found.getId();
    }
}
