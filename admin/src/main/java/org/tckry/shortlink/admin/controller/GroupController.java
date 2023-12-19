package org.tckry.shortlink.admin.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.tckry.shortlink.admin.service.GroupService;

/**
 * 短链接分组控制层
 **/
@RestController
@RequiredArgsConstructor
public class GroupController {
    private final GroupService groupService;
}
