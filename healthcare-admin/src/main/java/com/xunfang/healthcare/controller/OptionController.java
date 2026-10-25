package com.xunfang.healthcare.controller;

import com.xunfang.healthcare.common.Result;
import com.xunfang.healthcare.service.OptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 业务下拉选项。
 * <p>这一组接口只要求登录、不做按钮级权限校验，因为开方、挂号、接诊这些一线业务
 * 本来就离不开科室、医生、药品、患者选项，而它们的管理接口属于管理员权限。</p>
 */
@Tag(name = "下拉选项")
@RestController
@RequestMapping("/api/common/options")
@RequiredArgsConstructor
public class OptionController {

    private final OptionService optionService;

    /**
     * 科室选项。
     */
    @Operation(summary = "科室选项")
    @GetMapping("/depts")
    public Result<List<Map<String, Object>>> depts() {
        return Result.success(optionService.listDepts());
    }

    /**
     * 医生选项。
     */
    @Operation(summary = "医生选项")
    @GetMapping("/doctors")
    public Result<List<Map<String, Object>>> doctors() {
        return Result.success(optionService.listDoctors());
    }

    /**
     * 药品选项。
     */
    @Operation(summary = "药品选项")
    @GetMapping("/drugs")
    public Result<List<Map<String, Object>>> drugs(@RequestParam(required = false) String keyword) {
        return Result.success(optionService.listDrugs(keyword));
    }

    /**
     * 患者选项。
     */
    @Operation(summary = "患者选项")
    @GetMapping("/patients")
    public Result<List<Map<String, Object>>> patients(@RequestParam(required = false) String keyword) {
        return Result.success(optionService.listPatients(keyword));
    }
}
