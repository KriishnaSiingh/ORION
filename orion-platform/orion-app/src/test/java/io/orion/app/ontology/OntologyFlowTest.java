package io.orion.app.ontology;

import io.orion.shared.tenant.CurrentUser;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import com.jayway.jsonpath.JsonPath;

import java.util.*;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class OntologyFlowTest {

    private static final String PASSWORD = "S3cure-passphrase!";
    private static final String OT = "/api/v1/ontology/object-types";
    private static final String LT = "/api/v1/ontology/link-types";

    @Autowired MockMvc mvc;
    @Autowired JdbcClient jdbc;

    record Session(String slug, String access) {}

    private static String q(String s) { return s.replace('\'', '"'); }

    private Session register() throws Exception {
        String slug = "t-" + UUID.randomUUID().toString().substring(0, 8);
        String json = mvc.perform(post("/api/v1/auth/register-tenant")
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("{\"tenantName\":\"%s\",\"tenantSlug\":\"%s\",\"adminEmail\":\"admin@example.com\",\"adminName\":\"Admin\",\"password\":\"%s\"}",
                        slug, slug, PASSWORD)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return new Session(slug, JsonPath.read(json, "$.accessToken"));
    }

    private String tokenFor(Session admin, String email, String role) throws Exception {
        mvc.perform(post("/api/v1/users").header("Authorization", "Bearer " + admin.access())
                .contentType(MediaType.APPLICATION_JSON)
                .content(q("{'email':'%s','name':'User','password':'%s','roles':['%s']}").formatted(email, PASSWORD, role)))
                .andExpect(status().isCreated());
        String json = mvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(q("{'tenantSlug':'%s','email':'%s','password':'%s'}").formatted(admin.slug(), email, PASSWORD)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.accessToken");
    }

    private ResultActions doGet(String token, String url) throws Exception {
        return mvc.perform(get(url).header("Authorization", "Bearer " + token));
    }
    private ResultActions doPost(String token, String url, String json) throws Exception {
        return mvc.perform(post(url).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }
    private ResultActions doPut(String token, String url, String json) throws Exception {
        return mvc.perform(put(url).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }
    private ResultActions doDelete(String token, String url) throws Exception {
        return mvc.perform(delete(url).header("Authorization", "Bearer " + token));
    }

    private String id(ResultActions r) throws Exception {
        return JsonPath.read(r.andReturn().getResponse().getContentAsString(), "$.id");
    }

    private String objectType(String name) {
        return q("{'apiName':'%s','displayName':'%s','color':'#3366FF'}").formatted(name, name);
    }
    private String createType(String token, String name) throws Exception {
        return id(doPost(token, OT, objectType(name)).andExpect(status().isCreated()));
    }
    private String link(String name, String source, String target) {
        return q("{'apiName':'%s','displayName':'%s','sourceObjectTypeId':'%s','targetObjectTypeId':'%s','cardinality':'ONE_TO_MANY'}")
                .formatted(name, name, source, target);
    }
    private String propUpdate(boolean required, long revision) {
        return q("{'displayName':'Prop','required':%s,'unique':false,'searchable':false,'filterable':false,'expectedRevision':%d}")
                .formatted(required, revision);
    }

    @Test
    void designerBuildsABankOntology_andSnapshotReflectsIt() throws Exception {
        Session a = register();
        String person = createType(a.access(), "Person");
        String account = createType(a.access(), "BankAccount");

        doPost(a.access(), OT + "/" + person + "/properties",
                q("{'apiName':'nationality','displayName':'Nationality','dataType':'ENUM','enumValues':['IN','US'],'filterable':true}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.enumValues.length()").value(2));
        doPost(a.access(), OT + "/" + person + "/properties",
                q("{'apiName':'name','displayName':'Name','dataType':'STRING','required':true,'searchable':true}"))
                .andExpect(status().isCreated());
        doPost(a.access(), LT, link("OWNS", person, account)).andExpect(status().isCreated());

        doGet(a.access(), "/api/v1/ontology").andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(5))
                .andExpect(jsonPath("$.objectTypes.length()").value(2))
                .andExpect(jsonPath("$.linkTypes.length()").value(1))
                .andExpect(jsonPath("$.objectTypes[*].properties[*].apiName", containsInAnyOrder("name", "nationality")));
    }

    @Test
    void ontologiesAreTenantIsolated() throws Exception {
        Session a = register();
        Session b = register();
        String aPerson = createType(a.access(), "Person");

        doGet(b.access(), OT + "/" + aPerson).andExpect(status().isNotFound());
        doPut(b.access(), OT + "/" + aPerson, q("{'displayName':'Hacked','expectedRevision':1}")).andExpect(status().isNotFound());

        String bPerson = createType(b.access(), "Person");
        doGet(b.access(), OT).andExpect(jsonPath("$.length()").value(1));
        doGet(a.access(), OT).andExpect(jsonPath("$.length()").value(1));

        doPost(b.access(), LT, link("KNOWS", bPerson, aPerson)).andExpect(status().isBadRequest());
    }

    @Test
    void onlyDesignersAndAdminsCanModify_everyoneCanRead() throws Exception {
        Session a = register();
        String analyst = tokenFor(a, "ann@example.com", "ANALYST");
        String designer = tokenFor(a, "dan@example.com", "ONTOLOGY_DESIGNER");

        doPost(analyst, OT, objectType("Person")).andExpect(status().isForbidden());
        doPost(designer, OT, objectType("Person")).andExpect(status().isCreated());
        doGet(analyst, OT).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        doGet(analyst, "/api/v1/ontology").andExpect(status().isOk());
    }

    @Test
    void invalidDefinitionsAreRejected() throws Exception {
        Session a = register();
        doPost(a.access(), OT, objectType("person")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("apiName"));
        doPost(a.access(), OT, q("{'apiName':'Person','displayName':'Person','color':'redH'}")).andExpect(status().isBadRequest());

        String url = OT + "/" + createType(a.access(), "Person") + "/properties";
        doPost(a.access(), url, q("{'apiName':'kind','displayName':'Kind','dataType':'ENUM'}"))
                .andExpect(status().isBadRequest());
        doPost(a.access(), url, q("{'apiName':'active','displayName':'Active','dataType':'BOOLEAN','unique':true}"))
                .andExpect(status().isBadRequest());
        doPost(a.access(), url, q("{'apiName':'age','displayName':'Age','dataType':'INTEGER','searchable':true}"))
                .andExpect(status().isBadRequest());
        doPost(a.access(), url, q("{'apiName':'id','displayName':'Id','dataType':'STRING'}"))
                .andExpect(status().isBadRequest());
        doPost(a.access(), url, q("{'apiName':'name','displayName':'Name','dataType':'STRING','enumValues':['x']}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void duplicatesConflict_andFailedEditsDoNotConsumeVersions() throws Exception {
        Session a = register();
        String person = createType(a.access(), "Person");
        doPost(a.access(), OT, objectType("Person")).andExpect(status().isConflict());

        String url = OT + "/" + person + "/properties";
        String prop = q("{'apiName':'name','displayName':'Name','dataType':'STRING'}");
        doPost(a.access(), url, prop).andExpect(status().isCreated());
        doPost(a.access(), url, prop).andExpect(status().isConflict());

        doGet(a.access(), "/api/v1/ontology").andExpect(jsonPath("$.version").value(2));
    }

    @Test
    void staleRevisionIsRejected() throws Exception {
        Session a = register();
        String person = createType(a.access(), "Person");

        doPut(a.access(), OT + "/" + person, q("{'displayName':'People','expectedRevision':1}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.revision").value(2));
        doPut(a.access(), OT + "/" + person, q("{'displayName':'Persons','expectedRevision':1}"))
                .andExpect(status().isConflict());
    }

    @Test
    void constraintsCanBeLoosenedButNotTightened() throws Exception {
        Session a = register();
        String url = OT + "/" + createType(a.access(), "Person") + "/properties";

        String optional = id(doPost(a.access(), url, q("{'apiName':'nickname','displayName':'Nick','dataType':'STRING'}"))
                .andExpect(status().isCreated()));
        doPut(a.access(), url + "/" + optional, propUpdate(true, 1)).andExpect(status().isBadRequest());

        String mandatory = id(doPost(a.access(), url, q("{'apiName':'fullName','displayName':'Name','dataType':'STRING','required':true}"))
                .andExpect(status().isCreated()));
        doPut(a.access(), url + "/" + mandatory, propUpdate(false, 1))
                .andExpect(status().isOk()).andExpect(jsonPath("$.revision").value(2))
                .andExpect(jsonPath("$.required").value(false));

        doPut(a.access(), url + "/" + mandatory, q("{'displayName':'x','expectedRevision':2}")).andExpect(status().isBadRequest());

        String kind = id(doPost(a.access(), url, q("{'apiName':'kind','displayName':'Kind','dataType':'ENUM','enumValues':['A','B']}"))
                .andExpect(status().isCreated()));
        String enumPut = q("{'displayName':'Kind','required':false,'unique':false,'searchable':false,'filterable':false,'enumValues':%s,'expectedRevision':%d}");
        doPut(a.access(), url + "/" + kind, enumPut.formatted(q("['A']"), 1)).andExpect(status().isBadRequest());
        doPut(a.access(), url + "/" + kind, enumPut.formatted(q("['A','B','C']"), 1)).andExpect(status().isOk());
    }

    @Test
    void archivingRespectsDependencies_andKeepsNames() throws Exception {
        Session a = register();
        String person = createType(a.access(), "Person");
        String account = createType(a.access(), "BankAccount");
        String owns = id(doPost(a.access(), LT, link("OWNS", person, account)).andExpect(status().isCreated()));

        doDelete(a.access(), OT + "/" + person).andExpect(status().isConflict());
        doDelete(a.access(), LT + "/" + owns).andExpect(status().isNoContent());
        doDelete(a.access(), OT + "/" + person).andExpect(status().isNoContent());

        doGet(a.access(), OT).andExpect(jsonPath("$.length()").value(1));
        doGet(a.access(), OT + "?includeArchived=true").andExpect(jsonPath("$.length()").value(2));
        doGet(a.access(), LT).andExpect(jsonPath("$.length()").value(0));

        doPut(a.access(), OT + "/" + person, q("{'displayName':'X','expectedRevision':2}")).andExpect(status().isConflict());
        doPost(a.access(), OT, objectType("Person")).andExpect(status().isConflict());
        doPost(a.access(), LT, link("KNOWS", person, account)).andExpect(status().isBadRequest());
    }

    @Test
    void changesAreLoggedAndAudited_andLedgerIsAppendOnly() throws Exception {
        Session a = register();
        String person = createType(a.access(), "Person");
        doPost(a.access(), OT + "/" + person + "/properties",
                q("{'apiName':'name','displayName':'Name','dataType':'STRING'}")).andExpect(status().isCreated());
        doPut(a.access(), OT + "/" + person, q("{'displayName':'People','expectedRevision':1}")).andExpect(status().isOk());

        doGet(a.access(), "/api/v1/ontology/changes").andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.items[0].ontologyVersion").value(3))
                .andExpect(jsonPath("$.items[0].changeType").value("UPDATED"))
                .andExpect(jsonPath("$.items[0].before.displayName").value("Person"))
                .andExpect(jsonPath("$.items[0].after.displayName").value("People"))
                .andExpect(jsonPath("$.items[2].changeType").value("CREATED"));

        for (String action : List.of("OBJECT_TYPE_CREATED", "PROPERTY_CREATED", "OBJECT_TYPE_UPDATED")) {
            doGet(a.access(), "/api/v1/audit/logs?action=" + action)
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.items[0].details.ontologyVersion").isNumber());
        }

        assertThatThrownBy(() -> jdbc.sql("UPDATE ontology_changes SET change_type = 'CREATED'").update())
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
        assertThatThrownBy(() -> jdbc.sql("DELETE FROM ontology_changes").update())
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
    }

    @Test
    void concurrentEditsAreSerialisedIntoContiguousVersions() throws Exception {
        Session a = register();
        try (ExecutorService pool = Executors.newFixedThreadPool(8)) {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < 12; i++) {
                String name = "Type" + (char) ('A' + i);
                futures.add(pool.submit(() -> {
                    doPost(a.access(), OT, objectType(name)).andExpect(status().isCreated());
                    return null;
                }));
            }
            for (Future<?> f : futures) f.get(30, TimeUnit.SECONDS);
        }
        doGet(a.access(), "/api/v1/ontology").andExpect(jsonPath("$.version").value(12));
        Long distinct = jdbc.sql("""
                SELECT count(DISTINCT ontology_version) FROM ontology_changes
                 WHERE tenant_id = (SELECT id FROM tenants WHERE slug = :s)""")
                .param("s", a.slug()).query(Long.class).single();
        assertThat(distinct).isEqualTo(12L);
    }
}
