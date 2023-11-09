package rs.raf;

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

        while(calendar.getTime()!=toDate){
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
        }


        calendar.setTime(fromDate);

        ClassLecture cl = new ClassLecture(lectureName, professor, startTime, duration, fromDate, toDate);
        while(calendar.getTime()!=toDate){
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
    public void RemoveClass(Schedule schedule,Date date,Date toDate, int startTime, String classroomName, String lectureName)
            throws DatesException,DurationException,ClassroomDoesntExistException,TermTakenException,WrongStartTimeException, InternalError{

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
            }
        }
        if(duration==0)
        {
            throw new ClassLectureDoesntExistException("ne postoji cas sa zadatim podacima");
        }

        while(calendar.getTime()!=toDate){
            for(Map.Entry<Term,ClassLecture> entry : schedule.getScheduleMap().entrySet()){
                for(int i = 0; i<duration; i++){
                    if(entry.getKey().getDate().equals(calendar.getTime()) && entry.getKey().getClassroom().getName().equals(classroomName)
                            && entry.getKey().getStartTime() == startTime+i)
                    {
                        schedule.getScheduleMap().put(entry.getKey(),null);
                    }
                }
            }
            calendar.add(Calendar.DAY_OF_MONTH, 7);
        }

    }




    @Override
    public void RescheduleClass(Schedule schedule, Date oldDate,Date oldToDate, int oldStartTime, String oldClassroomName, String lectureName, Date newDate,Date newToDate, int newStartTime, String newClassroomName)
            throws WrongStartTimeException,DatesException, WrongDateException,WrongLectureNameException, ClassroomDoesntExistException, ClassLectureDoesntExistException, TermTakenException{

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

        while(calendar.getTime()!=newToDate){
            count = 0;
            for(Map.Entry<Term,ClassLecture> entry : schedule.getScheduleMap().entrySet()){
                for(int i =0 ;i<duration; i++){
                    if(entry.getKey().getDate().equals(calendar.getTime()) && entry.getKey().getClassroom().getName().equals(newClassroomName)
                            && entry.getKey().getStartTime() == newStartTime+i){
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
                throw new TermTakenException("ne postoji slobodan termin");
            }
            calendar.add(Calendar.DAY_OF_MONTH, 7);
        }

        ClassLecture cl2 = new ClassLecture(lectureName, cl.getProfessor(), newStartTime, duration, newDate, newToDate);
        List<Term> termini = new ArrayList<>();

        while(calendar.getTime()!=newToDate){
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

        while(calendar2.getTime()!=oldToDate){
            for(Map.Entry<Term,ClassLecture> entry : schedule.getScheduleMap().entrySet()){
                for(int i = 0; i<duration; i++){
                    if(entry.getKey().getDate().equals(calendar2.getTime()) && entry.getKey().getClassroom().getName().equals(oldClassroomName)
                            && entry.getKey().getStartTime() == oldStartTime+i)
                    {
                        schedule.getScheduleMap().put(entry.getKey(),null);
                    }
                }
            }
            calendar.add(Calendar.DAY_OF_MONTH, 7);
        }

    }
}