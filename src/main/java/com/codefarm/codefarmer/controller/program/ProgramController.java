package com.codefarm.codefarmer.controller.program;

import com.codefarm.codefarmer.domain.member.MemberDTO;
import com.codefarm.codefarmer.domain.program.MemberProgramDTO;
import com.codefarm.codefarmer.domain.program.ProgramDTO;
import com.codefarm.codefarmer.domain.program.ProgramFileDTO;
import com.codefarm.codefarmer.entity.member.Member;
import com.codefarm.codefarmer.entity.program.MemberProgram;
import com.codefarm.codefarmer.entity.program.Program;
import com.codefarm.codefarmer.entity.program.ProgramFile;
import com.codefarm.codefarmer.repository.member.MemberRepository;
import com.codefarm.codefarmer.repository.program.ProgramFileRepository;
import com.codefarm.codefarmer.service.member.MemberService;
import com.codefarm.codefarmer.service.program.*;
import com.codefarm.codefarmer.type.ProgramLevel;
import com.codefarm.codefarmer.type.ProgramType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpSession;
import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.List;
import java.util.zip.DataFormatException;

@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/program/*")
public class ProgramController {

    private final ProgramListService programListService;
    private final ProgramDetailService programDetailService;
    private final ProgramRegisterService programRegisterService;
    private final ProgramFileRepository programFileRepository;
    private final ProgramUpdateService programUpdateService;
    private final ProgramDeleteService programDeleteService;
    private final MemberRepository memberRepository;
    private final MemberService memberService;

    @GetMapping("/list")
    public void list(Model model){
        List<ProgramDTO> lists = programListService.showAll();
        model.addAttribute("lists",lists);
    }

    @GetMapping("/detail")
    public void detail(Model model,@RequestParam Long programId){
        log.info("상세페이지 들어옴");
        log.info("programId:" + programId);
        ProgramDTO list = programDetailService.showByProgramId(programId);
        list.setFiles(programDetailService.showFiles(programId));
        log.info("리스트 내용: " );
//        List<ProgramDTO> lists = programListService.();
        model.addAttribute("list",list);
    }

//    프로그램 신청 페이지 이동
    @GetMapping("/apply")
    public void apply(HttpSession session ,@RequestParam Long programId , Model model){
        Long memberId = (Long)session.getAttribute("memberId");
        log.info("memberId:" + memberId);
        Member member = memberService.select(memberId);
        String memberName = member.getMemberName();
        ProgramDTO programDTO = programDetailService.showByProgramId(programId);
        log.info("프로그램DTO:" + programDTO.toString());
        String farmerName = memberService.select(programDTO.getMemberId()).getMemberName();
        String programName = programDTO.getProgramTitle();
        model.addAttribute("memberName" , memberName);
        model.addAttribute("farmerName" , farmerName);
        model.addAttribute("programName" , programName);
        model.addAttribute("programId" , programId);
    }



    @PostMapping("/pay")
    public void pay(HttpSession session, @RequestParam Long programId,Model model ,MemberProgramDTO memberProgramDTO,String programApplyBirthString){
        Long memberId = (Long)session.getAttribute("memberId");
        log.info("memberId:" + memberId);
        Member member = memberService.select(memberId);
        String memberName = member.getMemberName();
        ProgramDTO programDTO = programDetailService.showByProgramId(programId);
        log.info("프로그램DTO:" + programDTO.toString());
        String farmerName = memberService.select(programDTO.getMemberId()).getMemberName();
        int programPrice = programDTO.getProgramPrice();
        int programApplyCount = memberProgramDTO.getProgramApplyCount();
        int programApplyTotalCount = programDTO.getProgramApplyTotalCount();
        int programTotalPrice = programPrice * programApplyCount;
        String programName = programDTO.getProgramTitle();
        String memberEmail = memberService.select(programDTO.getMemberId()).getMemberEmail();
        String memberPhone = memberService.select(programDTO.getMemberId()).getMemberPhone();
        String memberLocation = memberService.select(programDTO.getMemberId()).getMemberLocation();
        model.addAttribute("memberName" , memberName);
        model.addAttribute("farmerName" , farmerName);
        model.addAttribute("programApplyCount" , programApplyCount);
        model.addAttribute("programApplyTotalCount" ,programApplyTotalCount);
        model.addAttribute("programPrice" , programPrice);
        model.addAttribute("programTotalPrice" , programTotalPrice);
        model.addAttribute("programName", programName);
        model.addAttribute("memberEmail", memberEmail);
        model.addAttribute("memberPhone", memberPhone);
        model.addAttribute("memberLocation", memberLocation);
        model.addAttribute("programApplyBirthString",programApplyBirthString);
        model.addAttribute("programId", programId);

//        model.addAttribute("programTotalPrice" , programDTO.getProgramPrice() * )


        log.info("첫번째 신청 테스트:" + memberProgramDTO.toString());
        log.info("programApplyBirthString" + programApplyBirthString);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDateTime programApplyBirth = LocalDate.parse(programApplyBirthString, formatter).atStartOfDay();
        memberProgramDTO.setProgramApplyBirth(programApplyBirth);
        model.addAttribute("programApplyBirth",programApplyBirth);

        log.info("멤버프로그램DTO:" + memberProgramDTO.toString());
    }

    @GetMapping("/register")
    public void register(Model model){
        model.addAttribute("programRegister" , new ProgramDTO());
    }

    @PostMapping("/register")
    public RedirectView registerFin(ProgramDTO programDTO,  HttpSession session ,String programWorkDateString, String programWorkStartTimeString, String programWorkEndTimeString, String programApplyStartDateString, String programApplyEndDateString , String programTypeString, String programLevelString) throws DateTimeParseException {
        log.info("리스폰스바디 컨트롤러 들어옴");
        Long sessionId = (Long)session.getAttribute("memberId");

//        log.info("fileDTO:" + programFileDTO.toString());

//       세션에 memberId 넣기
        programDTO.setMemberId(sessionId);

//        글 등록 시 일반인용,멘티전용 따라 DTO에 값 넣기
        if(programTypeString.equals("일반인용")){
            programDTO.setProgramType(ProgramType.ALL_USER);
        }else{
            programDTO.setProgramType(ProgramType.ONLY_MENTEE);
        }

//        글 등록 시 초급,중급,고급 따라 DTO에 값 넣기
        if(programLevelString.equals("초급")){
            programDTO.setProgramLevel(ProgramLevel.BASIC);
        }else if(programLevelString.equals("중급")){
            programDTO.setProgramLevel(ProgramLevel.SKILL_UP);
        }else{
            programDTO.setProgramLevel(ProgramLevel.MASTER);
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        DateTimeFormatter formatter1 = new DateTimeFormatterBuilder()
                .append(DateTimeFormatter.ISO_LOCAL_TIME)
                .parseDefaulting(ChronoField.EPOCH_DAY, 0)
                .toFormatter();

        LocalDateTime programWorkDateTest = LocalDate.parse(programWorkDateString, formatter).atStartOfDay();
        LocalDateTime programWorkStartTimeTest = LocalDateTime.parse(programWorkStartTimeString, formatter1);
        LocalDateTime programWorkEndTimeTest = LocalDateTime.parse(programWorkEndTimeString, formatter1);
        LocalDateTime programApplyStartDateTest = LocalDate.parse(programApplyStartDateString, formatter).atStartOfDay();
        LocalDateTime programApplyEndDateTest = LocalDate.parse(programApplyEndDateString, formatter).atStartOfDay();

        programDTO.setProgramWorkDate(programWorkDateTest);
        programDTO.setProgramWorkStartTime(programWorkStartTimeTest);
        programDTO.setProgramWorkEndTime(programWorkEndTimeTest);
        programDTO.setProgramApplyStartDate(programApplyStartDateTest);
        programDTO.setProgramApplyEndDate(programApplyEndDateTest);

        programRegisterService.saveAll(programDTO);
        return new RedirectView("list");
    }

//    업데이트 페이지 이동
    @GetMapping("/update")
    public void update(Model model , @RequestParam Long programId){
        ProgramDTO updateRegister = programUpdateService.showUpdate(programId);
        model.addAttribute("updateRegister" , updateRegister);
    }

//    프로그램 업데이트 제출하기 버튼 클릭 시
    @PostMapping("/update")
    public RedirectView updateFin(@RequestParam Long programId, ProgramDTO programDTO ,HttpSession session , String programWorkDateString, String programWorkStartTimeString, String programWorkEndTimeString, String programApplyStartDateString, String programApplyEndDateString , String programTypeString, String programLevelString){
        log.info("리스폰스바디 컨트롤러 들어옴");
        log.info("programTypeString: " + programTypeString);
        log.info("programLevelString: " + programLevelString);
        Long sessionId = (Long)session.getAttribute("memberId");

//       세션에 memberId 넣기
        programDTO.setMemberId(sessionId);

//        글 등록 시 일반인용,멘티전용 따라 DTO에 값 넣기
        if(programTypeString.equals("일반인용")){
            programDTO.setProgramType(ProgramType.ALL_USER);
        }else{
            programDTO.setProgramType(ProgramType.ONLY_MENTEE);
        }

//        글 등록 시 초급,중급,고급 따라 DTO에 값 넣기
        if(programLevelString.equals("초급")){
            programDTO.setProgramLevel(ProgramLevel.BASIC);
        }else if(programLevelString.equals("중급")){
            programDTO.setProgramLevel(ProgramLevel.SKILL_UP);
        }else{
            programDTO.setProgramLevel(ProgramLevel.MASTER);
        }

//        log.info("files: " + programDTO.getFiles());
//        programDTO.getFiles().stream().map(t -> programFileDTO.setFileName(t.getFileName()));
//        log.info("file[0].fileName:"+ programDTO.getFiles().get(0).getFileName());
//        log.info("file[0].fileName:"+ programDTO.getFiles().get(0).getFileUploadPath());
//        log.info("file[0].fileName:"+ programDTO.getFiles().get(0).getFileUuid());
//        log.info("file[0].fileName:"+ programDTO.getFiles().get(0).getFileSize());

//      태관 참고
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        DateTimeFormatter formatter1 = new DateTimeFormatterBuilder()
                .append(DateTimeFormatter.ISO_LOCAL_TIME)
                .parseDefaulting(ChronoField.EPOCH_DAY, 0)
                .toFormatter();

//        태관 참고
        LocalDateTime programWorkDateTest = LocalDate.parse(programWorkDateString, formatter).atStartOfDay();

        log.info("1" + programWorkDateTest);
        LocalDateTime programWorkStartTimeTest = LocalDateTime.parse(programWorkStartTimeString, formatter1);
        log.info("2" + programWorkStartTimeTest);
        LocalDateTime programWorkEndTimeTest = LocalDateTime.parse(programWorkEndTimeString, formatter1);
        log.info("3" + programWorkEndTimeTest);
        LocalDateTime programApplyStartDateTest = LocalDate.parse(programApplyStartDateString, formatter).atStartOfDay();
        LocalDateTime programApplyEndDateTest = LocalDate.parse(programApplyEndDateString, formatter).atStartOfDay();

//        태관 참고
        programDTO.setProgramWorkDate(programWorkDateTest);
        programDTO.setProgramWorkStartTime(programWorkStartTimeTest);
        programDTO.setProgramWorkEndTime(programWorkEndTimeTest);
        programDTO.setProgramApplyStartDate(programApplyStartDateTest);
        programDTO.setProgramApplyEndDate(programApplyEndDateTest);

        /*Program program = null;
        program.changeFiles(programDTO.getFiles());*/

//        태관 참고
        log.info("프로그램id 가져오는지? : " + programDTO.getProgramId());


        programDTO.setProgramId(programId);
        programUpdateService.update(programDTO);



//        programDTO.getFiles().stream().map(t -> programFileRepository.saveAll(t));


//        programFileRepository.save(programDTO.getFiles())

//        redirectAttributes.addFlashAttribute("boardNumber", boardVO.getBoardNumber());
        return new RedirectView("list");
    }

    @PostMapping("/delete")
    public String delete(String programIdString){
        log.info("delete컨트롤러 들어옴" );
        log.info("programId:" + programIdString);
        Long programId = Long.valueOf(programIdString);
//        File file = new File("C:/upload", uploadPath + "/" + fileName);
//        if(file.exists()){
//            file.delete();
//        }
//
//        if(fileImageCheck) {
//            file = new File("C:/upload", uploadPath + "/s_" + fileName);
//            if (file.exists()) {
//                file.delete();
//            }
//        }
        programDeleteService.programDelete(programId);
        return "redirect:/mypage/program";
    }
}
