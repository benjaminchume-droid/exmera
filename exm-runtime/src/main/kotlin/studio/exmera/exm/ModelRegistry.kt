package studio.exmera.exm

/**
 * Curated model catalog. Only models whose exact code/checkpoint license has been
 * reviewed as redistributable should move to APPROVED_FOR_BUNDLING.
 */
enum class ModelLicenseStatus { APPROVED_FOR_BUNDLING, REVIEW_REQUIRED, COMMERCIAL_LICENSE_REQUIRED, NOT_FOR_DISTRIBUTION }

data class ExmModelSpec(
    val id: String,
    val displayName: String,
    val task: String,
    val sourceUrl: String,
    val license: String,
    val status: ModelLicenseStatus,
    val preferredPack: String,
    val inputNotes: String = "",
    val checkpointNotes: String = ""
)

object ExmModelRegistry {
    val approved: List<ExmModelSpec> = listOf(
        ExmModelSpec("sam2", "Segment Anything 2", "image/video segmentation and tracking", "https://github.com/facebookresearch/sam2", "Apache-2.0", ModelLicenseStatus.APPROVED_FOR_BUNDLING, "creator", checkpointNotes = "Repository states model checkpoints are Apache-2.0."),
        ExmModelSpec("real-esrgan", "Real-ESRGAN", "image/video restoration and super-resolution", "https://github.com/xinntao/Real-ESRGAN", "BSD-3-Clause", ModelLicenseStatus.APPROVED_FOR_BUNDLING, "core", checkpointNotes = "Verify the selected checkpoint and include its notices."),
        ExmModelSpec("mmpose", "MMPose", "human pose and landmarks", "https://github.com/open-mmlab/mmpose", "Apache-2.0", ModelLicenseStatus.APPROVED_FOR_BUNDLING, "smart", checkpointNotes = "Select a checkpoint whose model/data terms are compatible with redistribution."),
        ExmModelSpec("mmsegmentation", "MMSegmentation", "semantic segmentation", "https://github.com/open-mmlab/mmsegmentation", "Apache-2.0", ModelLicenseStatus.APPROVED_FOR_BUNDLING, "smart", checkpointNotes = "Check any model-specific or dataset-specific terms."),
        ExmModelSpec("opencv", "OpenCV", "classical vision primitives", "https://opencv.org/", "Apache-2.0", ModelLicenseStatus.APPROVED_FOR_BUNDLING, "core"),
    )

    val reviewRequired: List<ExmModelSpec> = listOf(
        ExmModelSpec("paddleocr", "PaddleOCR", "OCR and document understanding", "https://github.com/PaddlePaddle/PaddleOCR", "Apache-2.0", ModelLicenseStatus.REVIEW_REQUIRED, "smart", checkpointNotes = "Repository license is permissive; audit the exact pretrained model and bundled assets before distribution."),
        ExmModelSpec("raft", "RAFT", "dense optical flow", "https://github.com/princeton-vl/RAFT", "BSD-style", ModelLicenseStatus.REVIEW_REQUIRED, "video", checkpointNotes = "Audit the exact repository revision and pretrained checkpoint terms."),
        ExmModelSpec("rife", "RIFE", "video frame interpolation", "https://github.com/hzwer/ECCV2022-RIFE", "MIT-style repository terms", ModelLicenseStatus.REVIEW_REQUIRED, "video", checkpointNotes = "Audit the exact checkpoint and release terms before bundling."),
        ExmModelSpec("depth-anything", "Depth Anything", "monocular depth estimation", "https://github.com/LiheYoung/Depth-Anything", "Model-specific", ModelLicenseStatus.REVIEW_REQUIRED, "smart", checkpointNotes = "Select and audit a checkpoint with explicit commercial redistribution rights."),
        ExmModelSpec("dinov2", "DINOv2", "visual embeddings and similarity", "https://github.com/facebookresearch/dinov2", "Model-specific", ModelLicenseStatus.REVIEW_REQUIRED, "smart", checkpointNotes = "Audit the exact checkpoint terms."),
    )

    val blockedUntilLicensed: List<ExmModelSpec> = listOf(
        ExmModelSpec("insightface-weights", "InsightFace pretrained weights", "face recognition", "https://github.com/deepinsight/insightface", "Model-specific", ModelLicenseStatus.COMMERCIAL_LICENSE_REQUIRED, "smart", checkpointNotes = "Do not ship pretrained weights until commercial rights are obtained."),
        ExmModelSpec("ultralytics-yolo", "Ultralytics YOLO", "object detection", "https://github.com/ultralytics/ultralytics", "AGPL-3.0 / Enterprise", ModelLicenseStatus.COMMERCIAL_LICENSE_REQUIRED, "core", checkpointNotes = "Use only under a compatible commercial license for a proprietary Exmera distribution."),
    )

    fun find(id: String): ExmModelSpec? = (approved + reviewRequired + blockedUntilLicensed).firstOrNull { it.id == id }
    fun canBundle(id: String): Boolean = find(id)?.status == ModelLicenseStatus.APPROVED_FOR_BUNDLING
}
