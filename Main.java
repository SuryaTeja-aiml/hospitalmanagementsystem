import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

class HospitalSystemException extends Exception {
    public HospitalSystemException(String message) {
        super(message);
    }
}

class PatientNotFoundException extends HospitalSystemException {
    public PatientNotFoundException(String patientId) {
        super("Patient with ID '" + patientId + "' not found.");
    }
}

class AppointmentNotFoundException extends HospitalSystemException {
    public AppointmentNotFoundException(String appointmentId) {
        super("Appointment with ID '" + appointmentId + "' not found.");
    }
}

class Patient {
    String id;
    String name;
    int age;
    String gender;
    String contactNumber;

    public Patient(String id, String name, int age, String gender, String contactNumber) {
        if (name == null || name.trim().isEmpty() || age <= 0 || gender == null || gender.trim().isEmpty() || contactNumber == null || contactNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid patient data provided.");
        }
        this.id = id;
        this.name = name;
        this.age = age;
        this.gender = gender;
        this.contactNumber = contactNumber;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public int getAge() { return age; }
    public String getGender() { return gender; }
    public String getContactNumber() { return contactNumber; }

    @Override
    public String toString() {
        return String.format("Patient [ID=%-5s | Name=%-20s | Age=%-3d | Gender=%-10s | Contact=%-15s]",
                             id, name, age, gender, contactNumber);
    }

    public String toFormattedString() {
        return String.format("| %-5s | %-20s | %-5d | %-10s | %-15s |",
                             id, name, age, gender, contactNumber);
    }
}

class Appointment {
    String appointmentId;
    String patientId;
    String patientName;
    String doctorName;
    String date;
    String time;
    String reason;

    private static final AtomicInteger count = new AtomicInteger(0);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public Appointment(String patientId, String patientName, String doctorName, String date, String time, String reason) throws HospitalSystemException {
        if (patientId == null || patientId.trim().isEmpty() ||
            patientName == null || patientName.trim().isEmpty() ||
            doctorName == null || doctorName.trim().isEmpty() ||
            reason == null || reason.trim().isEmpty()) {
            throw new HospitalSystemException("Missing required appointment details.");
        }
        try {
            LocalDate.parse(date, DATE_FORMATTER);
            LocalTime.parse(time, TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new HospitalSystemException("Invalid date or time format. Use YYYY-MM-DD and HH:MM. " + e.getMessage());
        }

        this.appointmentId = "A" + count.incrementAndGet();
        this.patientId = patientId;
        this.patientName = patientName;
        this.doctorName = doctorName;
        this.date = date;
        this.time = time;
        this.reason = reason;
    }

    public String getAppointmentId() { return appointmentId; }
    public String getPatientId() { return patientId; }
    public String getPatientName() { return patientName; }
    public String getDoctorName() { return doctorName; }
    public String getDate() { return date; }
    public String getTime() { return time; }
    public String getReason() { return reason; }

    @Override
    public String toString() {
        return String.format("Appointment [ID=%-5s | PatientID=%-5s | Name=%-15s | Doctor=%-15s | Date=%-10s | Time=%-5s | Reason=%-20s]",
               appointmentId, patientId, patientName, doctorName, date, time, reason);
    }

    public String toFormattedString() {
        return String.format("| %-7s | %-10s | %-20s | %-15s | %-10s | %-8s | %-25s |",
                             appointmentId, patientId, patientName, doctorName, date, time, reason);
    }
}

class AuthService {
    private final Map<String, String> users = new HashMap<>();
    private final Map<String, String> roles = new HashMap<>();

    public AuthService() {
        initializeUsers();
    }

    private void initializeUsers() {
        users.put("admin", "admin123");
        roles.put("admin", "Admin");
        users.put("reception", "pass123");
        roles.put("reception", "Receptionist");
    }

    public String login(String username, String password) {
        if (users.containsKey(username) && users.get(username).equals(password)) {
            return roles.get(username);
        }
        return null;
    }

    public Map<String, String> getAllUserRoles() {
        return Collections.unmodifiableMap(roles);
    }
}

class PatientService {
    private final List<Patient> patients = new ArrayList<>();
    private final AtomicInteger patientIdCounter = new AtomicInteger(0);

    public PatientService() {
        try {
            registerPatient("John Doe", 30, "Male", "555-1234");
            registerPatient("Jane Smith", 25, "Female", "555-5678");
        } catch (HospitalSystemException e) {
            System.err.println("Error initializing sample patients: " + e.getMessage());
        }
    }

    public Patient registerPatient(String name, int age, String gender, String contactNumber) throws HospitalSystemException {
        if (name == null || name.trim().isEmpty() ||
            gender == null || gender.trim().isEmpty() ||
            contactNumber == null || contactNumber.trim().isEmpty() ||
            age <= 0) {
            throw new HospitalSystemException("Invalid input. Name, gender, contact cannot be empty, and age must be positive.");
        }

        String id = "P" + patientIdCounter.incrementAndGet();
        Patient newPatient;
        try {
            newPatient = new Patient(id, name, age, gender, contactNumber);
        } catch (IllegalArgumentException e) {
            throw new HospitalSystemException("Failed to create patient: " + e.getMessage());
        }
        patients.add(newPatient);
        System.out.println("Patient registered successfully! Assigned ID: " + id);
        return newPatient;
    }

    public Optional<Patient> findPatientById(String patientId) {
        return patients.stream()
                       .filter(p -> p.getId().equalsIgnoreCase(patientId))
                       .findFirst();
    }

    public List<Patient> getAllPatients() {
        return Collections.unmodifiableList(patients);
    }
}

class AppointmentService {
    private final List<Appointment> appointments = new ArrayList<>();
    private final PatientService patientService;

    public AppointmentService(PatientService patientService) {
        this.patientService = patientService;
    }

    public Appointment bookAppointment(String patientId, String doctorName, String date, String time, String reason) throws HospitalSystemException {
        Patient patient = patientService.findPatientById(patientId)
                                        .orElseThrow(() -> new PatientNotFoundException(patientId));
        Appointment newAppointment = new Appointment(patient.getId(), patient.getName(), doctorName, date, time, reason);
        appointments.add(newAppointment);
        System.out.println("Appointment booked successfully!");
        return newAppointment;
    }

    public List<Appointment> getAppointmentsByPatientId(String patientId) throws PatientNotFoundException {
        patientService.findPatientById(patientId)
                      .orElseThrow(() -> new PatientNotFoundException(patientId));
        return appointments.stream()
                           .filter(a -> a.getPatientId().equalsIgnoreCase(patientId))
                           .collect(Collectors.toList());
    }

    public List<Appointment> getAllAppointments() {
        return Collections.unmodifiableList(appointments);
    }

    public boolean cancelAppointment(String appointmentId) throws AppointmentNotFoundException {
        Appointment appointmentToRemove = appointments.stream()
                .filter(a -> a.getAppointmentId().equalsIgnoreCase(appointmentId))
                .findFirst()
                .orElseThrow(() -> new AppointmentNotFoundException(appointmentId));
        return appointments.remove(appointmentToRemove);
    }
}

class ConsoleUtil {
    private static final Scanner scanner = new Scanner(System.in);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public static String getStringInput(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine();
    }

    public static String getNonEmptyStringInput(String prompt) {
        String input;
        while (true) {
            System.out.print(prompt);
            input = scanner.nextLine().trim();
            if (!input.isEmpty()) {
                return input;
            } else {
                System.out.println("Input cannot be empty. Please try again.");
            }
        }
    }

    public static int getIntInput(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                int value = Integer.parseInt(scanner.nextLine());
                return value;
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a whole number.");
            }
        }
    }

    public static int getPositiveIntInput(String prompt) {
        while (true) {
            int value = getIntInput(prompt);
            if (value > 0) {
                return value;
            } else {
                System.out.println("Invalid input. Please enter a positive number.");
            }
        }
    }

    public static String getDateInput(String prompt) {
        String dateStr;
        while (true) {
            dateStr = getNonEmptyStringInput(prompt);
            try {
                LocalDate.parse(dateStr, DATE_FORMATTER);
                return dateStr;
            } catch (DateTimeParseException e) {
                System.out.println("Invalid date format. Please use YYYY-MM-DD.");
            }
        }
    }

    public static String getTimeInput(String prompt) {
        String timeStr;
        while (true) {
            timeStr = getNonEmptyStringInput(prompt);
            try {
                LocalTime.parse(timeStr, TIME_FORMATTER);
                return timeStr;
            } catch (DateTimeParseException e) {
                System.out.println("Invalid time format. Please use HH:MM (24-hour).");
            }
        }
    }

    public static void pressEnterToContinue() {
        System.out.print("\nPress Enter to continue...");
        scanner.nextLine();
    }

    public static void closeScanner() {
        scanner.close();
    }
}

public class Main {
    private static final AuthService authService = new AuthService();
    private static final PatientService patientService = new PatientService();
    private static final AppointmentService appointmentService = new AppointmentService(patientService);

    private static final int REGISTER_PATIENT = 1;
    private static final int BOOK_APPOINTMENT = 2;
    private static final int VIEW_APPOINTMENTS_BY_PATIENT = 3;
    private static final int CANCEL_APPOINTMENT = 4;
    private static final int VIEW_ALL_PATIENTS = 5;
    private static final int VIEW_ALL_APPOINTMENTS = 6;
    private static final int VIEW_USER_ROLES = 7;
    private static final int LOGOUT = 0;

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("   Welcome to Enhanced Hospital System");
        System.out.println("========================================");

        String loggedInRole = login();

        if (loggedInRole != null) {
            System.out.println("\nLogin Successful. Welcome, " + loggedInRole + "!");
            showMenu(loggedInRole);
        } else {
            System.out.println("\nLogin failed. Exiting application.");
        }

        ConsoleUtil.closeScanner();
        System.out.println("\nThank you for using the Hospital System. Goodbye!");
    }

    private static String login() {
        int attempts = 3;
        while (attempts > 0) {
            System.out.println("\nPlease Login (Attempts left: " + attempts + ")");
            String username = ConsoleUtil.getStringInput("Username: ");
            String password = ConsoleUtil.getStringInput("Password: ");
            String role = authService.login(username, password);
            if (role != null) {
                return role;
            } else {
                System.out.println("Invalid username or password.");
                attempts--;
            }
        }
        return null;
    }

    private static void showMenu(String role) {
        int choice = -1;
        while (choice != LOGOUT) {
            displayMenuOptions(role);
            choice = ConsoleUtil.getIntInput("Enter your choice: ");

            try {
                switch (choice) {
                    case REGISTER_PATIENT:
                        handleRegisterPatient();
                        break;
                    case BOOK_APPOINTMENT:
                        handleBookAppointment();
                        break;
                    case VIEW_APPOINTMENTS_BY_PATIENT:
                        handleViewAppointmentsByPatientId();
                        break;
                    case CANCEL_APPOINTMENT:
                        handleCancelAppointment();
                        break;
                    case VIEW_ALL_PATIENTS:
                        if ("Admin".equals(role)) {
                            handleViewAllPatients();
                        } else {
                            System.out.println("Access Denied. Admin role required.");
                        }
                        break;
                    case VIEW_ALL_APPOINTMENTS:
                        if ("Admin".equals(role)) {
                            handleViewAllAppointments();
                        } else {
                            System.out.println("Access Denied. Admin role required.");
                        }
                        break;
                    case VIEW_USER_ROLES:
                        if ("Admin".equals(role)) {
                            handleViewUserRoles();
                        } else {
                            System.out.println("Access Denied. Admin role required.");
                        }
                        break;
                    case LOGOUT:
                        System.out.println("Logging out...");
                        break;
                    default:
                        System.out.println("Invalid choice. Please try again.");
                }
            } catch (HospitalSystemException e) {
                System.err.println("\nOperation Failed: " + e.getMessage());
            } catch (Exception e) {
                System.err.println("\nAn unexpected error occurred: " + e.getMessage());
                e.printStackTrace();
            }

            if (choice != LOGOUT) {
                ConsoleUtil.pressEnterToContinue();
            }
        }
    }

    private static void displayMenuOptions(String role) {
        System.out.println("\n--- " + role + " Menu ---");
        System.out.println(REGISTER_PATIENT + ". Register New Patient");
        System.out.println(BOOK_APPOINTMENT + ". Book New Appointment");
        System.out.println(VIEW_APPOINTMENTS_BY_PATIENT + ". View Appointments by Patient ID");
        System.out.println(CANCEL_APPOINTMENT + ". Cancel Appointment");
        if ("Admin".equals(role)) {
            System.out.println(VIEW_ALL_PATIENTS + ". View All Patients (Admin)");
            System.out.println(VIEW_ALL_APPOINTMENTS + ". View All Appointments (Admin)");
            System.out.println(VIEW_USER_ROLES + ". View User Roles (Admin)");
        }
        System.out.println(LOGOUT + ". Logout");
    }

    private static void handleRegisterPatient() throws HospitalSystemException {
        System.out.println("\n--- Register New Patient ---");
        String name = ConsoleUtil.getNonEmptyStringInput("Enter Patient Name: ");
        int age = ConsoleUtil.getPositiveIntInput("Enter Patient Age: ");
        String gender = ConsoleUtil.getNonEmptyStringInput("Enter Patient Gender (Male/Female/Other): ");
        String contact = ConsoleUtil.getNonEmptyStringInput("Enter Patient Contact Number: ");
        Patient newPatient = patientService.registerPatient(name, age, gender, contact);
        System.out.println("Details: " + newPatient.toString());
    }

    private static void handleBookAppointment() throws HospitalSystemException {
        System.out.println("\n--- Book New Appointment ---");
        String patientId = ConsoleUtil.getNonEmptyStringInput("Enter Patient ID: ");
        Patient patient = patientService.findPatientById(patientId)
                .orElseThrow(() -> new PatientNotFoundException(patientId));
        System.out.println("Booking for Patient: " + patient.getName() + " (ID: " + patient.getId() + ")");
        String doctorName = ConsoleUtil.getNonEmptyStringInput("Enter Doctor Name: ");
        String date = ConsoleUtil.getDateInput("Enter Appointment Date (YYYY-MM-DD): ");
        String time = ConsoleUtil.getTimeInput("Enter Appointment Time (HH:MM - 24hr format): ");
        String reason = ConsoleUtil.getNonEmptyStringInput("Enter Reason for Appointment: ");
        Appointment newAppointment = appointmentService.bookAppointment(patientId, doctorName, date, time, reason);
        System.out.println("Details: " + newAppointment.toString());
    }

    private static void handleViewAppointmentsByPatientId() throws HospitalSystemException {
        System.out.println("\n--- View Appointments by Patient ID ---");
        String searchId = ConsoleUtil.getNonEmptyStringInput("Enter Patient ID to view appointments: ");
        Patient patient = patientService.findPatientById(searchId)
                                      .orElseThrow(() -> new PatientNotFoundException(searchId));
        List<Appointment> patientAppointments = appointmentService.getAppointmentsByPatientId(searchId);
        System.out.println("\nAppointments for Patient: " + patient.getName() + " (ID: " + patient.getId() + ")");
        if (patientAppointments.isEmpty()) {
            System.out.println("No appointments found for this patient.");
        } else {
            System.out.println("-------------------------------------------------------------------------------------------------------------------------");
            System.out.printf("| %-7s | %-10s | %-20s | %-15s | %-10s | %-8s | %-25s |%n",
                              "App. ID", "Patient ID", "Patient Name", "Doctor", "Date", "Time", "Reason");
            System.out.println("-------------------------------------------------------------------------------------------------------------------------");
            for (Appointment a : patientAppointments) {
                System.out.println(a.toFormattedString());
            }
            System.out.println("-------------------------------------------------------------------------------------------------------------------------");
            System.out.println("Total appointments found: " + patientAppointments.size());
        }
    }

    private static void handleCancelAppointment() throws HospitalSystemException {
        System.out.println("\n--- Cancel Appointment ---");
        String appointmentId = ConsoleUtil.getNonEmptyStringInput("Enter Appointment ID to cancel: ");
        boolean cancelled = appointmentService.cancelAppointment(appointmentId);
        if (cancelled) {
            System.out.println("Appointment ID '" + appointmentId + "' cancelled successfully.");
        } else {
            System.out.println("Failed to cancel appointment ID '" + appointmentId + "'. It might have already been cancelled or never existed.");
        }
    }

    private static void handleViewAllPatients() {
        System.out.println("\n--- View All Registered Patients ---");
        List<Patient> allPatients = patientService.getAllPatients();
        if (allPatients.isEmpty()) {
            System.out.println("No patients registered yet.");
            return;
        }
        System.out.println("Total Patients: " + allPatients.size());
        System.out.println("+-------+----------------------+-------+------------+-----------------+");
        System.out.printf("| %-5s | %-20s | %-5s | %-10s | %-15s |%n", "ID", "Name", "Age", "Gender", "Contact");
        System.out.println("+-------+----------------------+-------+------------+-----------------+");
        for (Patient p : allPatients) {
            System.out.println(p.toFormattedString());
        }
        System.out.println("+-------+----------------------+-------+------------+-----------------+");
    }

    private static void handleViewAllAppointments() {
        System.out.println("\n--- View All Booked Appointments ---");
        List<Appointment> allAppointments = appointmentService.getAllAppointments();
        if (allAppointments.isEmpty()) {
            System.out.println("No appointments booked yet.");
            return;
        }
        System.out.println("Total Appointments: " + allAppointments.size());
        System.out.println("+---------+------------+----------------------+-----------------+------------+----------+---------------------------+");
        System.out.printf("| %-7s | %-10s | %-20s | %-15s | %-10s | %-8s | %-25s |%n",
                          "App. ID", "Patient ID", "Patient Name", "Doctor", "Date", "Time", "Reason");
        System.out.println("+---------+------------+----------------------+-----------------+------------+----------+---------------------------+");
        for (Appointment a : allAppointments) {
            System.out.println(a.toFormattedString());
        }
        System.out.println("+---------+------------+----------------------+-----------------+------------+----------+---------------------------+");
    }

    private static void handleViewUserRoles() {
        System.out.println("\n--- System User Roles ---");
        Map<String, String> userRoles = authService.getAllUserRoles();
        System.out.println("---------------------------");
        System.out.printf("| %-15s | %-15s |%n", "Username", "Role");
        System.out.println("---------------------------");
        for (Map.Entry<String, String> entry : userRoles.entrySet()) {
            System.out.printf("| %-15s | %-15s |%n", entry.getKey(), entry.getValue());
        }
        System.out.println("---------------------------");
    }
}