package PatientCard;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class Client {
    FhirContext ctx;
    IGenericClient client;

    @PostConstruct
    void connectToClient() {
        this.ctx = FhirContext.forR4();
        this.ctx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);

        this.client = ctx.newRestfulGenericClient("http://localhost:8088/baseR4/");


    }
}
