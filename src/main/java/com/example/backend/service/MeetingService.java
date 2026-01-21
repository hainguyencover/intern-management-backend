package com.example.backend.service;

import com.example.backend.dto.request.CreateMeetingRequest;
import com.example.backend.dto.response.MeetingResponse;
import com.example.backend.entity.GroupMember;
import com.example.backend.entity.Meeting;
import com.example.backend.entity.ProgramGroup;
import com.example.backend.exception.NotFoundException;
import com.example.backend.repository.GroupMemberRepository;
import com.example.backend.repository.MeetingRepository;
import com.example.backend.repository.ProgramGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.backend.enums.NotificationType;
import com.example.backend.service.NotificationService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final ProgramGroupRepository programGroupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;

    @Transactional
    public MeetingResponse createMeeting(CreateMeetingRequest req) {
        ProgramGroup group = programGroupRepository.findById(req.getGroupId())
                .orElseThrow(() -> new NotFoundException("Group not found: " + req.getGroupId()));

        Meeting meeting = new Meeting();
        meeting.setGroup(group);
        meeting.setTitle(req.getTitle());
        meeting.setDescription(req.getDescription());
        meeting.setMeetingLink(req.getMeetingLink());
        meeting.setLocation(req.getLocation());
        meeting.setStartAt(req.getStartAt());
        meeting.setEndAt(req.getEndAt());

        Meeting saved = meetingRepository.save(meeting);

        // Lấy intern đang active trong group
        List<GroupMember> members = groupMemberRepository.findAllByGroup_IdAndLeftAtIsNull(group.getId());

        // Gửi mail
        String subject = "[Intern Management] Lịch họp mới: " + saved.getTitle();
        String content = ""
                + "Nhóm: " + group.getName() + "\n"
                + "Bắt đầu: " + saved.getStartAt() + "\n"
                + "Kết thúc: " + saved.getEndAt() + "\n"
                + "Link: " + (saved.getMeetingLink() == null ? "(không có)" : saved.getMeetingLink()) + "\n"
                + "Địa điểm: " + (saved.getLocation() == null ? "(không có)" : saved.getLocation()) + "\n"
                + "Mô tả: " + (saved.getDescription() == null ? "(không có)" : saved.getDescription()) + "\n";

        for (GroupMember m : members) {
            var internUser = m.getIntern().getUser();

            // Gửi mail
            String to = internUser.getEmail();
            emailService.sendMeetingEmail(to, subject, content);

            // Tạo thông báo trong app
            notificationService.create(
                    internUser,
                    NotificationType.TASK,
                    "Lịch họp mới: " + saved.getTitle(),
                    "Nhóm: " + group.getName() + " | Bắt đầu: " + saved.getStartAt()
            );
        }


        return new MeetingResponse(
                saved.getId(),
                group.getId(),
                group.getName(),
                saved.getTitle(),
                saved.getDescription(),
                saved.getMeetingLink(),
                saved.getLocation(),
                saved.getStartAt(),
                saved.getEndAt()
        );
    }

    @Transactional(readOnly = true)
    public List<MeetingResponse> getMeetingsByGroup(Long groupId) {
        ProgramGroup group = programGroupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found: " + groupId));

        return meetingRepository.findByGroup_IdOrderByStartAtAsc(groupId).stream()
                .map(m -> new MeetingResponse(
                        m.getId(),
                        group.getId(),
                        group.getName(),
                        m.getTitle(),
                        m.getDescription(),
                        m.getMeetingLink(),
                        m.getLocation(),
                        m.getStartAt(),
                        m.getEndAt()
                ))
                .toList();
    }
}
