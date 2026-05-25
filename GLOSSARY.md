# Glossary

## Purpose

This file defines the repository's current domain language.

These definitions reflect the best current understanding from code and existing documentation. Where behavior is not yet manually verified, definitions describe observed intent rather than guaranteed runtime truth.

## Terms

### Coupio

The Android application in this repository. It is currently documented as a local-first, single-device coupon storage and manual issuance app.

### Coupon

A stored coupon record backed by:

- a local media asset
- optional metadata
- lifecycle timestamps
- a reuse policy
- a current lifecycle state

In code this is represented by `CouponEntity`.

### Coupon media

The file attached to a coupon. Current supported media types observed in code are:

- image
- PDF

### Admin

The privileged operator on the device who can:

- import coupons
- edit coupon metadata
- confirm pending coupons
- roll back pending coupons
- reissue locked coupons
- set the issuer name

The admin surface is PIN-gated in the current implementation.

### User

The non-admin viewer of the coupons tab who can browse visible coupons and share an available coupon.

### Issuer name

A stored display name used in user-facing status copy when the app refers to the person who can help with locked, expired, or pending coupons.

### Backstage

The UI label currently used for the admin area.

This is presentation language, not a separate system concept.

### Share to redeem

The current user-facing action label for starting coupon issuance. Sharing a coupon through Android share is the intended workflow for sending the coupon to another person, such as a partner. In the current implementation, this action moves a coupon from `AVAILABLE` to `PENDING`.

This is a documented workflow rule, even if the user-facing label may later be renamed.

### Pending window

The period after a coupon is shared and before it is finalized or rolled back.

The current default is one hour, observed in code via `DEFAULT_PENDING_WINDOW_MILLIS`, but this window may later become configurable.

### Confirm pending

The admin action that finalizes a pending coupon after it has been shared.

Depending on reuse policy, confirmation may move the coupon to `LOCKED` or back to `AVAILABLE`.
For cooldown-based policies, the cooldown window starts when the pending coupon is confirmed.

### Roll back

The admin action that cancels a pending coupon and returns it to `AVAILABLE`.

### Reissue

The action that returns a locked coupon to `AVAILABLE`, either manually or after a cooldown expires.

Expired coupons may also be manually reissued under the current documented product rules.

### Reuse policy

The rule that determines what happens after a pending coupon is confirmed.

Current policies observed in code:

- `SINGLE_USE`
- `DAILY`
- `WEEKLY`
- `MONTHLY`
- `ALWAYS`

Under the current documented rules, `SINGLE_USE` means single use until an admin manually reissues the coupon. It does not mean permanently non-reissuable.
For `DAILY`, `WEEKLY`, and `MONTHLY`, the lock window is anchored to confirmation time rather than the original share request.

### Available

The coupon state in which the coupon is visible as redeemable and can be shared.

### Pending

The coupon state entered immediately after share. The coupon is waiting for admin confirmation or rollback, or later automatic finalization.

### Locked

The coupon state after confirmation when the coupon is no longer immediately available. It may be permanently locked or waiting for later reissue depending on policy.

### Expired

Not a distinct stored coupon state. It is derived from the coupon's expiry date relative to the current local date.

Under the current documented rules, expiry does not prevent manual admin reissue.

### Reconciliation

The process that updates coupons whose pending or locked timing thresholds have already passed.

Current observed reconciliation surfaces:

- repository-level batch reconciliation
- a WorkManager worker
- periodic ViewModel polling while the app is open
