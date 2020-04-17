package de.coronavirus.imis;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.coronavirus.imis.api.dto.CreateInstitutionDTO;
import de.coronavirus.imis.api.dto.CreateLabTestDTO;
import de.coronavirus.imis.api.dto.CreatePatientDTO;
import de.coronavirus.imis.api.dto.UpdateTestStatusDTO;
import de.coronavirus.imis.config.domain.User;
import de.coronavirus.imis.config.domain.UserRepository;
import de.coronavirus.imis.config.domain.UserRole;
import de.coronavirus.imis.domain.LabTest;
import de.coronavirus.imis.domain.LabTestEventType;
import de.coronavirus.imis.domain.Laboratory;
import de.coronavirus.imis.domain.TestType;
import de.coronavirus.imis.services.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.*;


@Component
@Slf4j
@RequiredArgsConstructor
public class TestDataLoader implements ApplicationRunner {


	private final PatientService patientService;
	private final InstitutionService institutionService;
	private final LabTestService labTestService;
	private final PatientEventService eventService;
	private final StatsService statsService;
	private final UserRepository userRepository;
	private final PasswordEncoder encoder;

	static <T> Object makeDTO(String testFileName, Class<T> clazz)
			throws IOException {

		ObjectMapper mapper = new ObjectMapper();
		final String string = getAsString(testFileName);
		return mapper.readValue(string, clazz);
	}

	static String getAsString(String resourcePath) throws IOException {
		final String fullResourcePath = "sample_data" + File.separator + resourcePath;
		// getResourceAsStream never throws IOException. Instead it returns null
		final InputStream inputStream = TestDataLoader.class.getClassLoader().getResourceAsStream(fullResourcePath);
		if (inputStream == null) {
			throw new IOException("Ressource " + resourcePath + " nicht vorhanden.");
		}
		try (
				final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))
		) {
			final StringBuilder content = new StringBuilder();
			var line = bufferedReader.readLine();
			while (line != null) {
				content.append(line).append(System.lineSeparator());
				line = bufferedReader.readLine();
			}
			return content.toString();
		} catch (Exception e) {
			throw new IOException("Konnte Ressource " + resourcePath + " nicht lesen.", e);
		}
	}


	public void run(ApplicationArguments args) {
		log.info("Creating test data");
		try {

			log.info("Inserting patients");
			for (int i = 0; i < 100; i++) {
				var createPersonDTO = (CreatePatientDTO) makeDTO("persons" + File.separator + "person" + i + ".json", CreatePatientDTO.class);
				patientService.addPatient(createPersonDTO);
			}

			// SETUP OUR WORLD
			log.info("Inserting laboratory");
			var createLaboratoryDTO = (CreateInstitutionDTO) makeDTO("createLaboratory.json", CreateInstitutionDTO.class);
			var laboratory = institutionService.addInstitution(createLaboratoryDTO);

			log.info("Inserting doctor");
			var createDoctorsOfficeDTO = (CreateInstitutionDTO) makeDTO("createDoctorsOffice.json", CreateInstitutionDTO.class);
			var doctorsOffice = institutionService.addInstitution(createDoctorsOfficeDTO);

			log.info("Inserting test site");
			var createTestSiteDTO = (CreateInstitutionDTO) makeDTO("createTestSite.json", CreateInstitutionDTO.class);
			var testSite = institutionService.addInstitution(createTestSiteDTO);

			log.info("Inserting clinic");
			var createClinicDTO = (CreateInstitutionDTO) makeDTO("createClinic.json", CreateInstitutionDTO.class);
			var clinic = institutionService.addInstitution(createClinicDTO);

			log.info("Inserting department of health");
			var createDepartmentOfHealthDTO = (CreateInstitutionDTO) makeDTO("createDepartmentOfHealth.json", CreateInstitutionDTO.class);
			var departmentOfHealth = institutionService.addInstitution(createDepartmentOfHealthDTO);


			var user = User.builder()
					.userRole(UserRole.USER_ROLE_ADMIN)
					.username("test_lab")
					.firstName("Max")
					.lastName("Mustermann")
					.institution(laboratory)
					.password(encoder.encode("asdf"))
					.build();
			userRepository.saveAndFlush(user);

			var testDoc = User.builder()
					.username("test_doctor")
					.firstName("Sabine")
					.lastName("Musterfrau")
					.institution(doctorsOffice)
					.password(encoder.encode("asdf"))
					.userRole(UserRole.USER_ROLE_ADMIN)
					.build();
			userRepository.saveAndFlush(testDoc);
			var testTestingSiteUser = User.builder()
					.username("test_testing_site")
					.institution(testSite)
					.firstName("Norbert")
					.lastName("Mustermann")
					.password(encoder.encode("asdf"))
					.userRole(UserRole.USER_ROLE_ADMIN)
					.build();
			userRepository.saveAndFlush(testTestingSiteUser);
			var departmentOfHealthUser = User.builder()
					.username("test_department_of_health")
					.institution(departmentOfHealth)
					.firstName("Hildegard")
					.lastName("Musterfrau")
					.password(encoder.encode("asdf"))
					.userRole(UserRole.USER_ROLE_ADMIN)
					.build();
			userRepository.saveAndFlush(departmentOfHealthUser);
			// PERSON GETS SICK AND GOES TO THE DOCTOR
			// PERSON GETS REGISTERED
			var createPersonDTO = (CreatePatientDTO) makeDTO("createPerson.json", CreatePatientDTO.class);
			var person = patientService.addPatient(createPersonDTO);

			// THE DOCTOR CREATES AND SEND SAMPLE TO LAB
			// FIXME: What should be done here?
			// eventService.createScheduledEvent(person, laboratory.getId(), doctorsOffice.getId());

			// LAB RECEIVES SAMPLE AND PROCESSES IT
			final String testId = "42EF42";
			final String comment = "comment";
			var labTest = LabTest.builder()
					.laboratory((Laboratory) laboratory)
					.testId(testId)
					.comment(comment)
					.testType(TestType.PCR)
					.build();
			final CreateLabTestDTO createLabTestDTO = new CreateLabTestDTO();
			createLabTestDTO.setLaboratoryId(laboratory.getId());
			createLabTestDTO.setPatientCaseId(); // TODO: Create Case first
			createLabTestDTO.setTestId(testId);
			createLabTestDTO.setTestType(TestType.PCR);
			labTest = labTestService.createLabTest(createLabTestDTO);


			// LAB HAS RESULT AND SOTRES IT
			// FIXME: 22.03.20 report cannot be attached
			var updateTestStatus = UpdateTestStatusDTO.builder()
					.status(LabTestEventType.TEST_FINISHED_POSITIVE)
					.comment(comment)
					.testId(testId)
					.build();
			labTestService.updateTestStatus(laboratory.getId(), updateTestStatus);

			// HEALTH OFFICE WANTS TO SEE ALL DATA
			var allPatients = patientService.getAllPatients();

			// RKI WANTS SO SEE STATS FOR ZIP
			var patiensByZip = statsService.resultZipList("0", "9999999");


		} catch (IOException e) {
			log.error("Error creating test data", e);
		}
		log.info("Finished creating test data");
	}
}
