package rs.raf;

import java.io.*;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.opencsv.CSVWriter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import rs.raf.classes.ClassLecture;
import rs.raf.classes.Classroom;
import rs.raf.classes.Schedule;
import rs.raf.classes.Term;
import rs.raf.exceptions.*;
import rs.raf.schedule_management.ClassSchedule;
import rs.raf.schedule_management.ScheduleManager;

import java.util.*;

public class Implementation2 implements ClassSchedule {

    static {
        ScheduleManager.registerClassScheduler(new Implementation2());
    }

    @Override
    public void createClass(Schedule schedule, int startTime, int duration, String classroomName, String lectureName, String professor, Date fromDate, Date toDate)
            throws DatesException,DurationException,ClassroomDoesntExistException,TermTakenException,WrongStartTimeException, InternalError{

        if(fromDate.after(toDate)){
            throw new DatesException("Pocetni datum je posle zavrsnog datuma");
        }
        if(schedule.getStartHours()>startTime || schedule.getEndHours()<startTime){
            throw new WrongStartTimeException("Vreme koje ste dali je van radnih sati");
        }

        if(fromDate.before(schedule.getStartDate() ) || toDate.after(schedule.getEndDate())){
            throw new DatesException("Datum termina mora biti od: "+ schedule.getStartDate() + " do " + schedule.getEndDate());
        }

        if(duration<1){
            throw new DurationException("Trajanje mora biti minimum 1");
        }

        boolean flag = false;
        for(Classroom classroom : schedule.getClassrooms()){
            if(classroom.getName().equals(classroomName)){
                flag = true;
            }
        }
        if(!flag)
            throw new ClassroomDoesntExistException("Ne postoji ucionica sa ovim parametrima");

        int count = 0;

        List<Term> termini = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(fromDate);

        boolean firstDate = false;

//        System.out.println(toDate);
        while(true){
            System.out.println("-");
            System.out.println(toDate);
            System.out.println(calendar.getTime());
//            if((calendar.getTime()).equals(toDate)  || (calendar.getTime().after(toDate))){
//                toDate = calendar.getTime();
//                break;
//            }
            count = 0;
            for(Map.Entry<Term, ClassLecture> entry : schedule.getScheduleMap().entrySet()){
                for(int i =0 ;i<duration; i++){
                    if(entry.getKey().getDate().equals(calendar.getTime()) && entry.getKey().getClassroom().getName().equals(classroomName)
                            && entry.getKey().getStartTime() == startTime+i){
                        if(entry.getValue()==null){
                            count++;
                        }
                    }
                    if(count==duration)
                        break;
                }
                if(count==duration){
                    break;
                }
            }
            if(count!=duration && !firstDate)
            {
                throw new TermDoesntExistException("ne postoji slobodan termin");
            }
            else if(count!=duration){
                calendar.add(Calendar.DAY_OF_MONTH, -7);
                toDate= calendar.getTime();
                break;
            }
            firstDate = true;
            calendar.add(Calendar.DAY_OF_MONTH, 7);
            if(calendar.getTime().after(toDate)){
                calendar.add(Calendar.DAY_OF_MONTH, -7);
                toDate= calendar.getTime();
                break;
            }

        }


        System.out.println(toDate);


        calendar.setTime(fromDate);

        ClassLecture cl = new ClassLecture(lectureName, professor, startTime, duration, fromDate, toDate);

        while (!(calendar.getTime().after(toDate))){
            count = 0;
            for(Map.Entry<Term,ClassLecture> entry : schedule.getScheduleMap().entrySet()){
                for(int i =0 ;i<duration; i++){
                    if(entry.getKey().getDate().equals(calendar.getTime()) && entry.getKey().getClassroom().getName().equals(classroomName)
                            && entry.getKey().getStartTime() == startTime+i){
                        if(entry.getValue()==null){
                            count++;
                            termini.add(entry.getKey());
                        }
                    }
                    if(count==duration)
                        break;
                }
                if(count==duration) {
                    if(termini.isEmpty()){
                        throw new InternalError("Greska u bazi");
                    }
                    for(Term t : termini){
                        schedule.getScheduleMap().put(t,cl);
                    }
                    break;
                }
            }
            termini.clear();
            calendar.add(Calendar.DAY_OF_MONTH, 7);

        }
    }





    @Override
    public void removeClass(Schedule schedule,Date date,Date toDate, int startTime, String classroomName, String lectureName)
            throws DatesException,DurationException,ClassroomDoesntExistException,TermTakenException,WrongStartTimeException, InternalError{

        if(date.after(toDate)){
            throw new DatesException("Pocetni datum je posle zavrsnog datuma");
        }
        if(schedule.getStartHours()>startTime || schedule.getEndHours()<startTime){
            throw new WrongStartTimeException("Vreme koje ste dali je van radnih sati");
        }
        if(date.before(schedule.getStartDate()) || date.before(schedule.getStartDate())){
            throw new DatesException("Datum termina mora biti od: "+ schedule.getStartDate() + " do " + schedule.getEndDate());
        }
        boolean flag = false;
        for(Classroom classroom : schedule.getClassrooms()){
            if(classroom.getName().equals(classroomName)){
                flag = true;
            }
        }
        if(!flag)
            throw new ClassroomDoesntExistException("Ne postoji ucionica sa ovim parametrima");

        int duration = 0;

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        for(Map.Entry<Term,ClassLecture> entry : schedule.getScheduleMap().entrySet()){
            if(entry.getKey().getDate().equals(calendar.getTime()) && entry.getKey().getClassroom().getName().equals(classroomName)
                    && entry.getKey().getStartTime() == startTime && entry.getValue().getClassName().equals(lectureName)){
                duration = entry.getValue().getDuration();
                if(toDate.after(entry.getValue().getEndDate())){
                    toDate = entry.getValue().getEndDate();
                }

            }
        }
        if(duration==0)
        {
            throw new ClassLectureDoesntExistException("ne postoji cas sa zadatim podacima");
        }

        System.out.println(calendar.getTime());
        System.out.println(toDate);
        while(!(calendar.getTime().after(toDate))){
            for(Map.Entry<Term,ClassLecture> entry : schedule.getScheduleMap().entrySet()){
                for(int i = 0; i<duration; i++){
                    if(entry.getKey().getDate().equals(calendar.getTime()) && entry.getKey().getClassroom().getName().equals(classroomName)
                            && entry.getKey().getStartTime() == startTime+i && entry.getValue().getClassName().equals(lectureName))
                    {
                        schedule.getScheduleMap().put(entry.getKey(),null);
                    }
                }
            }

            calendar.add(Calendar.DAY_OF_MONTH, 7);
        }

    }




    @Override
    public void rescheduleClass(Schedule schedule, Date oldDate,Date oldToDate, int oldStartTime, String oldClassroomName, String lectureName, Date newDate,Date newToDate, int newStartTime, String newClassroomName)
            throws WrongStartTimeException,DatesException, WrongDateException,WrongLectureNameException, ClassroomDoesntExistException, ClassLectureDoesntExistException, TermTakenException{

        if(oldDate.after(oldToDate)){
            throw new DatesException("Pocetni datum je posle zavrsnog datuma");
        }
        if(newDate.after(newToDate)){
            throw new DatesException("Pocetni datum je posle zavrsnog datuma");
        }

        if(schedule.getStartHours()>oldStartTime || schedule.getEndHours()<oldStartTime || schedule.getStartHours()>newStartTime || schedule.getEndHours()<newStartTime){
            throw new WrongStartTimeException("Vreme koje ste dali je van radnih sati");
        }
        if(oldDate.before(schedule.getStartDate()) || oldDate.after(schedule.getEndDate()) || newDate.before(schedule.getStartDate()) || newDate.after(schedule.getEndDate())){
            throw new DatesException("Datum termina mora biti od: "+ schedule.getStartDate() + " do " + schedule.getEndDate());
        }
        boolean flag1 = false;
        boolean flag2 = false;
        for(Classroom classroom : schedule.getClassrooms()){
            if(classroom.getName().equals(oldClassroomName)){
                flag1 = true;
            }
            if(classroom.getName().equals(newClassroomName)){
                flag2 = true;
            }
        }
        if(!flag1 || !flag2)
            throw new ClassroomDoesntExistException("Ne postoji ucionica sa ovim parametrima");

        if(oldDate.getTime()-oldToDate.getTime() >= newDate.getTime()-newToDate.getTime()){
            throw new WrongDateException("Razmak izmedju novih datuma je mnogo mali");
        }

        int count =0;
        int duration = 0;
        ClassLecture cl = null;

        boolean firstDate = false;

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(newDate);

        Calendar calendar2 = Calendar.getInstance();
        calendar2.setTime(oldDate);

        for(Map.Entry<Term,ClassLecture> entry : schedule.getScheduleMap().entrySet()){
            if(entry.getKey().getDate().equals(oldDate) && entry.getKey().getClassroom().getName().equals(oldClassroomName)
                    && entry.getKey().getStartTime() == oldStartTime && entry.getValue().getClassName().equals(lectureName)){
                duration = entry.getValue().getDuration();
                cl = entry.getValue();
            }
        }

//        !(calendar.getTime().after(toDate))

        while(!(calendar.getTime().after(newToDate))){
            count = 0;
            for(Map.Entry<Term,ClassLecture> entry : schedule.getScheduleMap().entrySet()){
                for(int i =0 ;i<duration; i++){
                    if(entry.getKey().getDate().equals(calendar.getTime()) && entry.getKey().getClassroom().getName().equals(newClassroomName)
                            && entry.getKey().getStartTime() == newStartTime+i){
                        if(entry.getValue()==null || entry.getValue().getClassName().equals(lectureName)){
                            count++;
                        }
                    }
                    if(count==duration)
                        break;
                }
                if(count==duration){
                    break;
                }
            }
            if(count!=duration && !firstDate)
            {
                throw new TermTakenException("ne postoji slobodan termin");
            }
            calendar.add(Calendar.DAY_OF_MONTH, 7);
        }



        ClassLecture cl2 = new ClassLecture(lectureName, cl.getProfessor(), newStartTime, duration, newDate, newToDate);
        List<Term> termini = new ArrayList<>();

        calendar.setTime(newDate);

        while(!(calendar.getTime().after(newToDate))){
            count = 0;
            for(Map.Entry<Term,ClassLecture> entry : schedule.getScheduleMap().entrySet()){
                for(int i =0 ;i<duration; i++){
                    if(entry.getKey().getDate().equals(calendar.getTime()) && entry.getKey().getClassroom().getName().equals(newClassroomName)
                            && entry.getKey().getStartTime() == newStartTime+i){
                        if(entry.getValue()==null){
                            count++;
                            termini.add(entry.getKey());
                        }
                    }
                    if(count==duration)
                        break;
                }
                if(count==duration) {
                    if(termini.isEmpty()){
                        throw new InternalError("Greska u bazi");
                    }
                    for(Term t : termini){
                        schedule.getScheduleMap().put(t,cl2);
                    }
                    break;
                }
            }
            termini.clear();
            calendar.add(Calendar.DAY_OF_MONTH, 7);
        }

        System.out.println("hello");


        while(!(calendar2.getTime().after(oldToDate))){

            System.out.println(calendar2.getTime());
            System.out.println(oldToDate);
            for(Map.Entry<Term,ClassLecture> entry : schedule.getScheduleMap().entrySet()){
                for(int i = 0; i<duration; i++){
                    if(entry.getKey().getDate().equals(calendar2.getTime()) && entry.getKey().getClassroom().getName().equals(oldClassroomName)
                            && entry.getKey().getStartTime() == oldStartTime+i && entry.getValue().getClassName().equals(lectureName))
                    {
                        schedule.getScheduleMap().put(entry.getKey(),null);
                    }
                }
            }

            calendar2.add(Calendar.DAY_OF_MONTH, 7);
        }


    }


    @Override
    public void exportCSV(Schedule schedule, String filePath) {
        if(filePath.isEmpty()){
            throw new FilePathException("Greska sa file lokacijom");
        }
        if(schedule.getScheduleMap().isEmpty()){
            throw new ScheduleException("Pokusavate da exportujete prazan raspored");
        }

        List<String[]> data = new ArrayList<>();
        data.add(new String[]{"Naziv predavanja", "Profesor", "Ucionica", "Datum", "Vreme od", "Vreme do"});
        //        "Naziv predavanja","Profesor","Ucionica","Datum od", "Datum do","Vreme od","Vreme do"


        // todo sort
        List<Term> termList = new ArrayList<>();
        for(Map.Entry<Term,ClassLecture> entry : schedule.getScheduleMap().entrySet()){
            termList.add(entry.getKey());
        }
        Collections.sort(termList, Comparator
                .comparing(Term::getDate)
                .thenComparing(Term::getStartTime)
        );

        for(Term t : termList){
            if(schedule.getScheduleMap().get(t) == null){
                continue;
            }
            ClassLecture classLecture = schedule.getScheduleMap().get(t);
            if(t.getStartTime()==classLecture.getStartTime()){

                // changing the date format
//                Date dateFromUtilDate = t.getDate();
//
//                Instant instant = dateFromUtilDate.toInstant();
//                LocalDate localDate = instant.atZone(ZoneId.systemDefault()).toLocalDate();
//
//                String formattedDate = localDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));

                String formattedDate1 = formatDate(t.getDate());
                String formattedDate2 = formatDate(classLecture.getEndDate());

                data.add(new String[]{classLecture.getClassName(), classLecture.getProfessor(), t.getClassroom().getName(),
                        formattedDate1+ "-" + formattedDate2, t.getStartTime()+":00", (classLecture.getDuration()+t.getStartTime())+":00"});
            }

        }

        try {

            // Create directories if they don't exist
            File directory = new File(filePath).getParentFile();
            if (!directory.exists()) {
                directory.mkdirs();
            }

            try (CSVWriter writer = new CSVWriter(new FileWriter(filePath))) {
                // Writing all data to the CSV file
                writer.writeAll(data); // ovo prima stringove



            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    @Override
    public void importCSV(Schedule schedule, String filePath) {
        int duration;
        boolean flag = false;

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;

            // Read the file line by line
            while ((line = br.readLine()) != null) {
                // Split the line by the CSV delimiter (comma, in this case)
                String[] fields = line.split(",");

                // Process the fields
                for (int i = 0; i < fields.length; i++) {
                    // Remove leading and trailing spaces and quotation marks
                    //if
                    fields[i] = fields[i].trim().replaceAll("^\"|\"$", "");
                }
                if(flag){

                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");


                    String[] dates = fields[3].split("-");
                    Date fromDate = dateFormat.parse(dates[0]);
                    Date toDate = dateFormat.parse(dates[1]);

                    String[] start = fields[4].split(":");
                    String[] end = fields[5].split(":");
                    int s = Integer.parseInt(start[0]);
                    int e = Integer.parseInt(end[0]);
                    duration = e-s;

                    ClassLecture cl = new ClassLecture(fields[0],fields[1],s,duration,fromDate,toDate);

                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(fromDate);

                    List<Term> termini = new ArrayList<>();


                    while (!(calendar.getTime().after(toDate))){
                        int count = 0;
                        for(Map.Entry<Term,ClassLecture> entry : schedule.getScheduleMap().entrySet()){
                            for(int i =0 ;i<duration; i++){
                                if(entry.getKey().getDate().equals(calendar.getTime()) && entry.getKey().getClassroom().getName().equals(fields[2])
                                        && entry.getKey().getStartTime() == s+i){
                                    if(entry.getValue()==null){
                                        count++;
                                        termini.add(entry.getKey());
                                    }
                                }
                                if(count==duration)
                                    break;
                            }
                            if(count==duration) {
                                if(termini.isEmpty()){
                                    throw new InternalError("Greska u bazi");
                                }
                                for(Term t : termini){
                                    schedule.getScheduleMap().put(t,cl);
                                }
                                break;
                            }
                        }
                        termini.clear();
                        calendar.add(Calendar.DAY_OF_MONTH, 7);

                    }

                }

                flag=true;

            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void exportPDF(Schedule schedule, String filePath) {
        if(filePath.isEmpty()){
            throw new FilePathException("Greska sa file lokacijom");
        }
        if(schedule.getScheduleMap().isEmpty()){
            throw new ScheduleException("Pokusavate da exportujete prazan raspored");
        }
        try {
            File directory = new File(filePath).getParentFile();
            if (!directory.exists()) {
                directory.mkdirs();
            }

            try(PDDocument document = new PDDocument()){

                List<Map<String, Object>> dataList = convertMapToListOfMaps(schedule.getScheduleMap());

                for(Map<String,Object> entry: dataList){
                    PDPage page = new PDPage();
                    document.addPage(page);


                    try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {



                        contentStream.beginText();
                        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12); // Example font and size, adjust as needed
                        contentStream.newLineAtOffset(100, 700);

                        for (Map.Entry<String, Object> field : entry.entrySet()) {
                            contentStream.showText(field.getKey() + ": " + field.getValue().toString());
                            contentStream.newLineAtOffset(0, -20); // Adjust as needed
                        }
                        contentStream.endText();

                    }
                }
                document.save(filePath);
                System.out.println("PDF file exported successfully to: " + filePath);

            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void importPDF(Schedule schedule, String filePath) {


//        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
//
//        Date startDate = dateFormat.parse(date);
//        Date toDate = dateFormat.parse(todate);
//        Calendar calendar = Calendar.getInstance();
//        calendar.setTime(startDate);
//
//        ClassLecture cl = new ClassLecture(className,professor,startTime,duration,startDate,toDate);
//
//        while(!(calendar.getTime().after(toDate))){
//            for(Map.Entry<Term,ClassLecture> entry : schedule.getScheduleMap().entrySet()){
//                for(int i = 0; i<duration; i++){
//                    if(entry.getKey().getDate().equals(calendar.getTime()) && entry.getKey().getClassroom().getName().equals(classroom)
//                            && entry.getKey().getStartTime() == startTime+i)
//                    {
//                        schedule.getScheduleMap().put(entry.getKey(),cl);
//                    }
//                }
//            }
//
//            calendar.add(Calendar.DAY_OF_MONTH, 7);
//        }

    }

    @Override
    public void exportJSON(Schedule schedule, String filePath) {
        if(filePath.isEmpty()){
            throw new FilePathException("Greska sa file lokacijom");
        }
        if(schedule.getScheduleMap().isEmpty()){
            throw new ScheduleException("Pokusavate da exportujete prazan raspored");
        }
        try {
            File directory = new File(filePath).getParentFile();
            if (!directory.exists()) {
                directory.mkdirs();
            }
            try (FileWriter writer = new FileWriter(filePath)) {
                // Convert the entry set to a list of maps
                List<Map<String, Object>> dataList = convertMapToListOfMaps(schedule.getScheduleMap());

                new Gson().toJson(dataList, writer);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void importJSON(Schedule schedule, String filePath) {

        Gson gson = new Gson();


        try (FileReader fileReader = new FileReader(filePath)) {
            // Deserialize JSON data into JsonArray
            JsonArray jsonArray = gson.fromJson(fileReader, JsonArray.class);

            // Access the data
            for (JsonElement jsonElement : jsonArray) {
                JsonObject jsonObject = jsonElement.getAsJsonObject();

                // Access lecture and Term objects dynamically
                JsonObject lecture = jsonObject.getAsJsonObject("ClassLecture");
                int duration = lecture.getAsJsonPrimitive("duration").getAsInt();
                String professor = lecture.getAsJsonPrimitive("professor").getAsString();
                String className = lecture.getAsJsonPrimitive("className").getAsString();
                String todate = lecture.getAsJsonPrimitive("toDate").getAsString();

                JsonObject term = jsonObject.getAsJsonObject("Term");
                String date = term.getAsJsonPrimitive("date").getAsString();
                String classroom = term.getAsJsonPrimitive("classroom").getAsString();
                int startTime = term.getAsJsonPrimitive("startTime").getAsInt();

                SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");

                Date startDate = dateFormat.parse(date);
                Date toDate = dateFormat.parse(todate);
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(startDate);

                ClassLecture cl = new ClassLecture(className,professor,startTime,duration,startDate,toDate);

                while(!(calendar.getTime().after(toDate))){
                    for(Map.Entry<Term,ClassLecture> entry : schedule.getScheduleMap().entrySet()){
                        for(int i = 0; i<duration; i++){
                            if(entry.getKey().getDate().equals(calendar.getTime()) && entry.getKey().getClassroom().getName().equals(classroom)
                                    && entry.getKey().getStartTime() == startTime+i)
                            {
                                schedule.getScheduleMap().put(entry.getKey(),cl);
                            }
                        }
                    }

                    calendar.add(Calendar.DAY_OF_MONTH, 7);
                }
                // Print the data
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private List<Map<String, Object>> convertMapToListOfMaps(Map<Term, ClassLecture> data) {
        List<Map<String, Object>> result = new ArrayList<>();

        for (Map.Entry<Term, ClassLecture> entry : data.entrySet()) {
            if(entry.getValue()==null){
                continue;
            }
            if(entry.getValue().getStartTime() != entry.getKey().getStartTime()){
                continue;
            }
            Map<String,Object> map = new HashMap<>();

            // adding term json
            Term term = entry.getKey();

            Map<String,Object> termDetails = new HashMap<>();

            termDetails.put("classroom",term.getClassroom().getName());
            termDetails.put("startTime",term.getStartTime());
            termDetails.put("date",formatDate(term.getDate()));


            // adding lecture to json map
            ClassLecture classLecture = entry.getValue();


            Map<String,Object> classLectureDetails = new HashMap<>();
            classLectureDetails.put("className", classLecture.getClassName());
            classLectureDetails.put("professor", classLecture.getProfessor());
            classLectureDetails.put("duration", classLecture.getDuration());
            classLectureDetails.put("toDate", formatDate(classLecture.getEndDate()));


            map.put("ClassLecture", classLectureDetails);
            map.put("Term",termDetails);


            result.add(map);
        }

        return result;

    }

    private String formatDate(Date date){
        Date dateFromUtilDate = date;

        Instant instant = dateFromUtilDate.toInstant();
        LocalDate localDate = instant.atZone(ZoneId.systemDefault()).toLocalDate();

        String formattedDate = localDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));

        return formattedDate;
    }
}