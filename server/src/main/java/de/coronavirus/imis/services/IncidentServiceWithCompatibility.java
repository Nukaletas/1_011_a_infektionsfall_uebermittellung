package de.coronavirus.imis.services;

import de.coronavirus.imis.api.dto.CreateLabTestDTO;
import de.coronavirus.imis.api.dto.UpdateTestStatusDTO;
import de.coronavirus.imis.domain.*;
import de.coronavirus.imis.repositories.TestIncidentRepository;
import de.coronavirus.imis.repositories.LaboratoryRepository;
import de.coronavirus.imis.repositories.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;
import java.util.stream.Collectors;

/*
TODO
This class is called deprectaed because it enables the frontend to work with  LabTestDTOs and QuarantineDTOs
instead of natively using IncidentDTOs.
Next steps: Build an Incident API and migrate the Frontend.
 */
@Deprecated
@Service
public class IncidentServiceWithCompatibility {

	@Autowired
	TestIncidentRepository testIncidentRepo;
	@Autowired
	LaboratoryRepository laboratoryRepo;
	@Autowired
	PatientRepository patientRepo;
	@Autowired
	RandomService randomService;

	@Transactional
	public LabTest addIncident (CreateLabTestDTO info)
	{
		var incident = (TestIncident) new TestIncident()
				.setTestId(info.getTestId())
				.setComment(info.getComment())
				.setType(info.getTestType())
				.setStatus(TestStatus.TEST_SUBMITTED)
				.setLaboratory(laboratoryRepo.findById(info.getLaboratoryId()).orElseThrow(LaboratoryNotFoundException::new))
				.setEventType(EventType.TEST_SUBMITTED_IN_PROGRESS)
				.setPatient(patientRepo.findById(info.getPatientId()).orElseThrow(PatientNotFoundException::new))
				.setId(randomService.getRandomString(10));

		testIncidentRepo.saveAndFlush(incident);
		return new LabTest(incident);
	}

	@Transactional
	public LabTest updateIncident (String labrotoryId, UpdateTestStatusDTO update)
	{
		var incident = testIncidentRepo.findById(update.getTestId()).orElseThrow();
		var laboratory = laboratoryRepo.findById(labrotoryId).orElseThrow();
		incident
			.setLaboratory(laboratory)
			.setStatus(update.getStatus())
			.setComment(update.getComment())
			.setReport(update.getFile())
			.setEventType(testStatusToEvent(update.getStatus()));
		// ToDo bei setStatus und setEventType: Semantik? Sinnvoll so?

		testIncidentRepo.saveAndFlush(incident);
		return new LabTest(incident);
	}

	@Transactional
	public List<LabTest> getAllTestIncidentForPatient (String patientId)
	{
		var patient = patientRepo.findById(patientId).orElseThrow();
		var toFind = (TestIncident) new TestIncident().setPatient(patient);
		var incidents = testIncidentRepo.findAll(Example.of(toFind));
		return toLabTests(incidents);
	}

	@Transactional
	public List<LabTest> queryLabTestsById (String partialTestId)
	{
		var incidents = testIncidentRepo.findByTestIdContaining(partialTestId);
		return toLabTests(incidents);
	}

	private List<LabTest> toLabTests(List<TestIncident> incidents)
	{
		return incidents.stream().map( c -> new LabTest(c)).collect(Collectors.toList());
	}

	private EventType testStatusToEvent(TestStatus input) {
		EventType result;
		switch (input) {
			case TEST_NEGATIVE:
				result = EventType.TEST_FINISHED_NEGATIVE;
				break;
			case TEST_SUBMITTED:
			case TEST_IN_PROGRESS:
				result = EventType.TEST_SUBMITTED_IN_PROGRESS;
				break;
			case TEST_POSITIVE:
				result = EventType.TEST_FINISHED_POSITIVE;
				break;
			default:
				result = EventType.TEST_FINISHED_INVALID;

		}
		return result;
	}

	// Quarantine Incidents
/*
	public Incident addIncident (CreateQuarantinePeriodDTO info)
	{
		return null;
	}

	public Incident addIncident (UpdateQuarantinePeriodDTO info)
	{
		return null;
	}
*/
}
