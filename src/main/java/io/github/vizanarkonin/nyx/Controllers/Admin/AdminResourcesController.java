package io.github.vizanarkonin.nyx.Controllers.Admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import io.github.vizanarkonin.nyx.Controllers.ControllerBase;
import io.github.vizanarkonin.nyx.Handlers.FileFolderHandler;

@Controller
@RequestMapping("/admin/resources")
public class AdminResourcesController extends ControllerBase {

    @GetMapping("")
    public String index(Model model) {
        long    dataFolderTotalDiskSpace    = FileFolderHandler.getDataFolderPartitionTotalSizeInMb(),
                dataFolderFreeDiskSpace     = FileFolderHandler.getDataFolderPartitionFreeSpaceInMb(),
                dataFolderSize              = FileFolderHandler.getDataFolderSizeInMb(),
                tempFolderTotalDiskSpace    = FileFolderHandler.getTempFolderPartitionTotalSizeInMb(),
                tempFolderFreeDiskSpace     = FileFolderHandler.getTempFolderPartitionFreeSpaceInMb(),
                tempFolderSize              = FileFolderHandler.getTempFolderSizeInMb();

        model.addAttribute("dataFolderPath",                FileFolderHandler.getDataFolderPath());
        model.addAttribute("dataFolderTotalDiskSpace",      dataFolderTotalDiskSpace);
        model.addAttribute("dataFolderFreeDiskSpace",       dataFolderFreeDiskSpace);
        model.addAttribute("dataFolderSize",                dataFolderSize);
        model.addAttribute("dataFolderDiskUsagePercent",    (dataFolderTotalDiskSpace - dataFolderFreeDiskSpace) / (dataFolderTotalDiskSpace / 100));
        model.addAttribute("tempFolderPath",                FileFolderHandler.getTempFolderPath());
        model.addAttribute("tempFolderTotalDiskSpace",      tempFolderTotalDiskSpace);
        model.addAttribute("tempFolderFreeDiskSpace",       tempFolderFreeDiskSpace);
        model.addAttribute("tempFolderSize",                tempFolderSize);
        model.addAttribute("tempFolderDiskUsagePercent",    (tempFolderTotalDiskSpace - tempFolderFreeDiskSpace) / (tempFolderTotalDiskSpace / 100));

        return "admin/resources";
    }

    @PostMapping("purgeTemp")
    public String deleteProject(RedirectAttributes redirectAttrs) {
        FileFolderHandler.purgeTempFolder();
        redirectAttrs.addFlashAttribute("successMessage", "Temp folder was successfully purged");

        return "redirect:/admin/resources";
    }
}
