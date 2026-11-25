# Security and privacy boundaries

The community edition uses public league data and device-local storage. It contains no production account configuration, administrative credentials, private service endpoints, watcher controls, signing keys, or Google sign-in/push enrollment.

Never include credentials or personal data in an issue or pull request. For a vulnerability involving sensitive details, use GitHub's private vulnerability reporting feature if available; otherwise start with a brief non-sensitive request for a private reporting channel. Do not publish an exploit containing someone else's identifiers or tokens.

Review third-party dependency and public-provider changes before running them. The Gradle distribution checksum is pinned. Forks must supply their own signing keys and any infrastructure they intentionally add.

Public source is not an access mechanism for any other FPPuck edition or its services. This is a software boundary, not a promise that software is free of vulnerabilities.
