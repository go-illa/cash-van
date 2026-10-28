package com.illa.cashvan.sales.domain.cashvan

// DIS-517 / DIS-543: the pure parts of the Fawry-device questionnaire. The
// Supabase reads and writes stay in the data layer.

/** PostgREST error code for a UNIQUE constraint violation. */
const val UNIQUE_VIOLATION_CODE = "23505"

/**
 * Whether to show the questionnaire before an order can be submitted: skip once
 * the merchant has a device on file or has agreed to get one (terminal); show
 * when never asked or declined before. A null [status] (read failed or no row)
 * fails open, so checkout is never blocked on a read error.
 */
fun needsFawryQuestionnaire(status: MerchantFawryStatus?): Boolean =
    status != null && status.fawryDeviceNumber == null && status.wantsFawryDevice != true

/** A Fawry device number already assigned to a different merchant (DB UNIQUE constraint). */
class DuplicateFawryDeviceNumberException :
    Exception("This Fawry device number is already assigned to another merchant")

/**
 * The `merchants` columns written when saving a device number. DIS-543:
 * `wants_fawry_device` is reset to null in the same write, since "wants a
 * device" means nothing once one is on file.
 */
fun fawryDeviceNumberUpdate(deviceNumber: String): Map<String, Any?> = mapOf(
    "fawry_device_number" to deviceNumber,
    "wants_fawry_device" to null,
)

/** The `merchants` columns written for a questionnaire answer; the device number is left untouched. */
fun fawryDeviceWantedUpdate(wants: Boolean): Map<String, Any?> = mapOf("wants_fawry_device" to wants)

/** Maps a failed device-number write to the error the PWA throws. */
fun fawryDeviceNumberSaveError(code: String?, message: String): Exception =
    if (code == UNIQUE_VIOLATION_CODE) DuplicateFawryDeviceNumberException() else Exception(message)
