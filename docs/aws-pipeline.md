# AWS CI/CD Pipeline

The workflow `.github/workflows/aws-gradle-pipeline.yml` builds the Android app and bridge library, uploads release artifacts to S3, publishes the `:bridge` module to AWS CodeArtifact, and (optionally) uploads the release APK to AWS Device Farm.

## Triggers

| Event | What runs |
|---|---|
| `push` to `main` | All four jobs |
| `pull_request` to `main` | `build` job only (tests + artifact upload) |
| `workflow_dispatch` | All four jobs |

## Jobs

| Job | Condition | Description |
|---|---|---|
| `build` | Always | Gradle build, unit tests, APK/AAB assembly, GitHub artifact upload |
| `publish_s3` | `main` branch only | Creates S3 bucket if missing; uploads artifacts + manifest |
| `publish_codeartifact` | `main` + CA vars set | Publishes `:bridge` (`com.queenfi.aster:bridge:0.1.0`) to CodeArtifact |
| `device_farm` | `main` + ARN secrets set | Uploads release APK to Device Farm as `ANDROID_APP` |

---

## Required GitHub Secrets

Add these in **Settings → Secrets and variables → Actions → Secrets**.

| Secret | Description |
|---|---|
| `AWS_ACCESS_KEY_ID` | Long-lived AWS access key ID |
| `AWS_SECRET_ACCESS_KEY` | Long-lived AWS secret access key |
| `DEVICE_FARM_PROJECT_ARN` | *(optional)* Device Farm project ARN — enables the `device_farm` job |
| `DEVICE_FARM_DEVICE_POOL_ARN` | *(optional)* Device Farm device pool ARN — enables the `device_farm` job |

---

## Required GitHub Variables

Add these in **Settings → Secrets and variables → Actions → Variables**.

The `publish_codeartifact` job is **skipped automatically** when any of the three CodeArtifact variables are absent or empty.

| Variable | Example | Description |
|---|---|---|
| `CODEARTIFACT_DOMAIN` | `my-domain` | CodeArtifact domain name |
| `CODEARTIFACT_REPOSITORY` | `my-repo` | CodeArtifact repository name |
| `CODEARTIFACT_DOMAIN_OWNER` | `475559502137` | 12-digit AWS account ID that owns the domain |

---

## S3 Artifact Layout

On every push to `main`, artifacts are uploaded to:

```
s3://queenfi703-amazon-iap-artifacts-us-east-1/
  builds/<owner>/<repo>/<git-sha>/
    *.apk
    *.aab
    release-manifest.json
```

`release-manifest.json` contains:

```json
{
  "repo": "<owner>/<repo>",
  "sha": "<git-sha>",
  "run_id": "<github-run-id>",
  "bucket": "queenfi703-amazon-iap-artifacts-us-east-1",
  "prefix": "builds/<owner>/<repo>/<git-sha>",
  "region": "us-east-1"
}
```

The bucket is created automatically in `us-east-1` if it does not already exist.

---

## CodeArtifact Maven coordinates

The `:bridge` module is published as:

```
groupId:    com.queenfi.aster
artifactId: bridge
version:    0.1.0
```

The Maven endpoint URL is constructed from the three GitHub Variables above:

```
https://<CODEARTIFACT_DOMAIN>-<CODEARTIFACT_DOMAIN_OWNER>.d.codeartifact.us-east-1.amazonaws.com/maven/<CODEARTIFACT_REPOSITORY>/
```

---

## Device Farm follow-up

The current implementation uploads the release APK as an `ANDROID_APP` upload. Scheduling a test run requires:
1. A compiled test APK (instrumentation test package).
2. Choosing a test framework (`INSTRUMENTATION`, `XCTEST`, etc.).
3. Calling `aws devicefarm schedule-run` with the app ARN, test ARN, device pool ARN, and run configuration.

Refer to the [Device Farm documentation](https://docs.aws.amazon.com/devicefarm/latest/developerguide/how-to-create-test-run.html) to complete this step.

---

## Required IAM permissions

The IAM user whose keys are stored in `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` must have:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "S3Artifacts",
      "Effect": "Allow",
      "Action": [
        "s3:CreateBucket",
        "s3:HeadBucket",
        "s3:PutObject",
        "s3:ListBucket",
        "s3:GetBucketLocation"
      ],
      "Resource": [
        "arn:aws:s3:::queenfi703-amazon-iap-artifacts-us-east-1",
        "arn:aws:s3:::queenfi703-amazon-iap-artifacts-us-east-1/*"
      ]
    },
    {
      "Sid": "CodeArtifactPublish",
      "Effect": "Allow",
      "Action": [
        "codeartifact:GetAuthorizationToken",
        "codeartifact:PublishPackageVersion",
        "sts:GetServiceBearerToken"
      ],
      "Resource": "*"
    },
    {
      "Sid": "DeviceFarmUpload",
      "Effect": "Allow",
      "Action": [
        "devicefarm:CreateUpload",
        "devicefarm:GetUpload"
      ],
      "Resource": "*"
    }
  ]
}
```
