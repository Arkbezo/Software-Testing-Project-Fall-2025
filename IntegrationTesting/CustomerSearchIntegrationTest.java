package IntegrationTesting;

import dal.DALManager;
import model.dto.CustomerDTO;
import model.dto.Response;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class CustomerSearchIntegrationTest{

    private DALManager dalManager;

    private final List<CustomerDTO> inserted = new ArrayList<>(); // keep local references for cleanup

    private String tag; // unique marker to avoid collisions in DB

    @BeforeEach
    void setUp() {
        dalManager = new DALManager();
        tag = "IntTest_" + UUID.randomUUID().toString().substring(0, 6);
    }

    @AfterEach
    void tearDown() {
        // IMPORTANT: delete requires valid IDs.
        // If IDs are still 0, try to sync them from DB one last time before deleting.
        try {
            ArrayList<CustomerDTO> dbResults = searchSafely(tag);
            syncIdsForAllInsertedSafely(dbResults);
        } catch (Exception ignored) {
            // even if sync fails, we still attempt deletion below
        }

        for (CustomerDTO c : inserted) {
            if (c.getId() <= 0) {
                // skip hard failure on cleanup if we couldn't obtain the ID
                // (but in normal run, IDs will be set and this won't happen)
                continue;
            }

            Response res = new Response();
            dalManager.deleteCustomer(c, res);
            assertTrue(res.isSuccessfull(),
                    "Cleanup delete failed for id=" + c.getId() + ": " + res.getErrorMessages());
        }

        inserted.clear();
    }

    @Test
    void insert_search_update_search_delete_complex_flow() {

        // 1) Insert 3 customers (2 should match tag+"_", 1 should NOT match that substring)
        CustomerDTO match1 = insertCustomer(tag + "_A", "0690000000");
        CustomerDTO match2 = insertCustomer(tag + "_B", "0690000001");
        CustomerDTO nonMatch = insertCustomer("OtherName_" + tag, "0690000002"); // doesn't contain tag+"_"

        // 2) Search once with broad term and sync IDs for ALL inserted customers
        ArrayList<CustomerDTO> allResults = search(tag);
        syncIdsForAllInserted(allResults);

        // 3) Search by the stricter substring tag+"_": should return match1 & match2 only
        String searchTerm = tag + "_";
        ArrayList<CustomerDTO> results1 = search(searchTerm);

        assertContainsByName(results1, match1.getName());
        assertContainsByName(results1, match2.getName());
        assertNotContainsByName(results1, nonMatch.getName());

        // 4) Update match1 name and verify search reflects change
        String oldName = match1.getName();
        String newName = tag + "_A_UPDATED";

        match1.setName(newName);

        Response updRes = new Response();
        dalManager.updateCustomer(match1, updRes);
        assertTrue(updRes.isSuccessfull(), "Update failed: " + updRes.getErrorMessages());

        // 5) Search again:
        // - new name should appear
        // - old name should not appear
        ArrayList<CustomerDTO> results2 = search(tag);

        assertContainsByName(results2, newName);
        assertNotContainsByName(results2, oldName);
    }

    // ---------------- helper methods ----------------

    private CustomerDTO insertCustomer(String name, String phone) {
        CustomerDTO c = new CustomerDTO();
        c.setName(name);
        c.setPhoneNumber(phone);

        Response res = new Response();
        dalManager.saveCustomer(c, res);

        assertTrue(res.isSuccessfull(), "Insert failed: " + res.getErrorMessages());

        // save reference for cleanup; id will be synced later from DB
        inserted.add(c);
        return c;
    }

    private ArrayList<CustomerDTO> search(String term) {
        Response res = new Response();
        ArrayList<CustomerDTO> results = dalManager.searchCustomersByName(term, res);

        assertTrue(res.isSuccessfull(), "Search failed: " + res.getErrorMessages());
        assertNotNull(results, "Results should not be null");

        return results;
    }

    // Safe versions used in teardown (avoid throwing assertion errors there)
    private ArrayList<CustomerDTO> searchSafely(String term) {
        Response res = new Response();
        ArrayList<CustomerDTO> results = dalManager.searchCustomersByName(term, res);
        return results == null ? new ArrayList<>() : results;
    }

    private void syncIdsForAllInserted(List<CustomerDTO> dbResults) {
        for (CustomerDTO local : inserted) {
            boolean matched = false;

            for (CustomerDTO db : dbResults) {
                if (local.getName() != null && local.getName().equals(db.getName())) {
                    local.setId(db.getId());
                    matched = true;
                    break;
                }
            }

            assertTrue(matched, "Failed to sync ID for customer: " + local.getName());
            assertTrue(local.getId() > 0, "Synced ID must be > 0 for: " + local.getName());
        }
    }

    private void syncIdsForAllInsertedSafely(List<CustomerDTO> dbResults) {
        for (CustomerDTO local : inserted) {
            if (local.getId() > 0) continue;
            for (CustomerDTO db : dbResults) {
                if (local.getName() != null && local.getName().equals(db.getName())) {
                    local.setId(db.getId());
                    break;
                }
            }
        }
    }

    private void assertContainsByName(List<CustomerDTO> list, String name) {
        boolean found = list.stream().anyMatch(c -> name != null && name.equals(c.getName()));
        assertTrue(found, "Expected to find name in results: " + name);
    }

    private void assertNotContainsByName(List<CustomerDTO> list, String name) {
        boolean found = list.stream().anyMatch(c -> name != null && name.equals(c.getName()));
        assertFalse(found, "Did NOT expect to find name in results: " + name);
    }
}


