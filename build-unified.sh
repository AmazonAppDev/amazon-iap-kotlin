#!/bin/bash
# Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
# SPDX-License-Identifier: MIT-0

set -e

echo "🔄 Fractal Operational Coherence: Building Unified Artifact"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# Phase 1: Init
echo "📍 Phase 1: Init (establish context)"
export BUILD_CONTEXT="aster-iap-bridge"
export BUILD_VERSION="0.1.0"

# Phase 2: Prepare (ensure gradlew is executable)
echo "📍 Phase 2: Prepare (compile Kotlin/JVM)"
chmod +x gradlew

# Phase 3: Execute (build all modules including the bridge)
echo "📍 Phase 3: Execute (build all Gradle modules)"
./gradlew build

# Phase 4: Report (generate coherence manifest)
echo "📍 Phase 4: Report (generate metadata)"
mkdir -p dist
cat > dist/COHERENCE_MANIFEST.json << 'EOF'
{
  "name": "amazon-iap-kotlin",
  "version": "0.1.0",
  "components": {
    "app": {
      "language": "Kotlin",
      "description": "Amazon IAP demo application"
    },
    "bridge": {
      "language": "Kotlin",
      "namespace": "com.queenfi.aster.bridge",
      "description": "Coherence bridge: ties external runtime context to Amazon IAP"
    }
  },
  "pattern": "Init → Prepare → Execute → Report → Cleanup",
  "substrate": "Android / Amazon Fire Tablet"
}
EOF

# Phase 5: Cleanup
echo "📍 Phase 5: Cleanup (finalize)"

echo ""
echo "✅ Build Complete"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "Artifacts:"
ls -lh dist/
