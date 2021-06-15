package PatientCard;

import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.ListIterator;

@Controller
public class PatientController {
    @Autowired
    Client mfc;

    @RequestMapping("/patient={id:.+}")
    public String patientData(@ModelAttribute Query query, @PathVariable("id") String id, Model model) {
        Patient patient;
        ArrayList<String[]> observations = new ArrayList<>();
        ArrayList<String[]> medicationRequests = new ArrayList<>();

        Bundle p = mfc.client
                .search()
                .forResource(Patient.class)
                .where(new TokenClientParam("_id").exactly().code(id))
                .returnBundle(Bundle.class)
                .execute();
        patient = (Patient) p.getEntry().get(0).getResource();

        Bundle o = this.mfc.client
                .search()
                .forResource(Observation.class)
                .where(new ReferenceClientParam("patient").hasId(id))
                .returnBundle(Bundle.class)
                .execute();

        try {
            getObseravtionsFormBundle(o, observations);
        } catch (FHIRException e) {
            e.printStackTrace();
        }

        while (o.getLink(Bundle.LINK_NEXT) != null) {
            // load next page
            o = this.mfc.client.loadPage().next(o).execute();
            try {
                getObseravtionsFormBundle(o, observations);
            } catch (FHIRException e) {
                e.printStackTrace();
            }
        }

        Collections.sort(observations,new Comparator<String[]>() {
            public int compare(String[] strings, String[] otherStrings) {
                return otherStrings[1].compareTo(strings[1]);
            }
        });

        Bundle m = this.mfc.client
                .search()
                .forResource(MedicationRequest.class)
                .where(new ReferenceClientParam("patient").hasId(id))
                .returnBundle(Bundle.class)
                .execute();

        try {
            getMedRequestFormBundle(m, medicationRequests);
        } catch (FHIRException e) {
            e.printStackTrace();
        }

        while (m.getLink(Bundle.LINK_NEXT) != null) {
            // load next page
            m = this.mfc.client.loadPage().next(m).execute();
            try {
                getMedRequestFormBundle(m, medicationRequests);
            } catch (FHIRException e) {
                e.printStackTrace();
            }
        }
        Collections.sort(medicationRequests,new Comparator<String[]>() {
            public int compare(String[] strings, String[] otherStrings) {
                return otherStrings[0].compareTo(strings[0]);
            }
        });

        model.addAttribute("patient", patient);
        model.addAttribute("observations", observations);
        model.addAttribute("medications", medicationRequests);
        model.addAttribute("id", id);
        model.addAttribute("query", new Query());
        model.addAttribute("time", query.getText());

        return "patient";
    }

    private void getObseravtionsFormBundle (Bundle bundle, ArrayList<String[]> list) throws FHIRException {
        for(ListIterator<Bundle.BundleEntryComponent> iter = bundle.getEntry().listIterator(); iter.hasNext(); ) {
            Observation observation = (Observation) iter.next().getResource();
            if(observation.getCode().hasText() && observation.hasValueQuantity()){
                String[] array = new String[4];
                array[0] = observation.getCode().getText();
                array[1] = observation.getEffectiveDateTimeType().toHumanDisplay();
                array[2] = String.valueOf(observation.getValueQuantity().getValue());
                array[3] = observation.getValueQuantity().getCode();
                list.add(array);
            }

        }
    }

    private void getMedRequestFormBundle (Bundle bundle, ArrayList<String[]> list) throws FHIRException {
        for(ListIterator<Bundle.BundleEntryComponent> iter = bundle.getEntry().listIterator(); iter.hasNext(); ) {
            MedicationRequest medicationRequest = (MedicationRequest) iter.next().getResource();
            String[] array = new String[3];
            array[0] = medicationRequest.getAuthoredOnElement().toHumanDisplay();
            array[1] = " ";//((CodeableConcept) medicationRequest.getExtension().get(0).getValue()).getText();
            if (medicationRequest.hasMedicationCodeableConcept()) {
                array[2] = medicationRequest.getMedicationCodeableConcept().getText();
            } else {
                array[2] = "";
            }
            list.add(array);

        }
    }
}
