#!/bin/bash
# HDFS HA Metadata Backup Script
# Enterprise-grade backup for NameNode and JournalNode

BACKUP_ROOT="/opt/hadoop/backups"
BACKUP_DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="${BACKUP_ROOT}/${BACKUP_DATE}"
RETENTION_DAYS=7
NAMENODE_DIR="/opt/hadoop/namenode"
JOURNALNODE_DIR="/opt/hadoop/journal"

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1"
}

log "===== Starting HDFS HA Backup ====="

# Create backup directory
mkdir -p "${BACKUP_DIR}"
log "Backup directory: ${BACKUP_DIR}"

# Step 1: Trigger checkpoint on Active NameNode
log "Step 1: Triggering NameNode checkpoint..."
ACTIVE_NN=""
for nn in nn1 nn2; do
    state=$(hdfs haadmin -getServiceState "${nn}" 2>/dev/null)
    if [ "${state}" = "active" ]; then
        ACTIVE_NN="${nn}"
        break
    fi
done

if [ -n "${ACTIVE_NN}" ]; then
    hdfs dfsadmin -saveNamespace
    hdfs dfsadmin -rollEdits
    log "Checkpoint completed on ${ACTIVE_NN}"
else
    log "WARNING: Could not find Active NameNode, skipping checkpoint"
fi

# Step 2: Backup NameNode metadata
log "Step 2: Backing up NameNode metadata..."
for node in node1 node2; do
    ssh "${node}" "if [ -d '${NAMENODE_DIR}' ]; then tar -czf - -C '$(dirname ${NAMENODE_DIR})' '$(basename ${NAMENODE_DIR})' 2>/dev/null; fi" > "${BACKUP_DIR}/namenode-${node}.tar.gz" 2>&1
    if [ $? -eq 0 ]; then
        log "✓ NameNode ${node} backup completed"
        ls -lh "${BACKUP_DIR}/namenode-${node}.tar.gz"
    else
        log "✗ NameNode ${node} backup failed or directory not found"
    fi
done

# Step 3: Backup JournalNode edits
log "Step 3: Backing up JournalNode edits..."
for node in node1 node2 node3; do
    ssh "${node}" "if [ -d '${JOURNALNODE_DIR}' ]; then tar -czf - -C '$(dirname ${JOURNALNODE_DIR})' '$(basename ${JOURNALNODE_DIR})' 2>/dev/null; fi" > "${BACKUP_DIR}/journalnode-${node}.tar.gz" 2>&1
    if [ $? -eq 0 ]; then
        log "✓ JournalNode ${node} backup completed"
        ls -lh "${BACKUP_DIR}/journalnode-${node}.tar.gz"
    else
        log "✗ JournalNode ${node} backup failed or directory not found"
    fi
done

# Step 4: Backup configuration files
log "Step 4: Backing up configuration files..."
tar -czf "${BACKUP_DIR}/hadoop-config.tar.gz" -C "/opt/hadoop/etc" hadoop 2>/dev/null
if [ $? -eq 0 ]; then
    log "✓ Configuration backup completed"
else
    log "✗ Configuration backup failed"
fi

# Step 5: Create backup manifest
cat > "${BACKUP_DIR}/manifest.txt" <<EOF
HDFS HA Backup Manifest
=======================
Date: ${BACKUP_DATE}
Cluster: mycluster
Backup Type: Full
EOF

hdfs haadmin -getAllServiceState >> "${BACKUP_DIR}/manifest.txt" 2>/dev/null
hdfs dfsadmin -report >> "${BACKUP_DIR}/manifest.txt" 2>/dev/null

# Step 6: Cleanup old backups
log "Step 6: Cleaning up old backups (retention: ${RETENTION_DAYS} days)..."
find "${BACKUP_ROOT}" -type d -mtime +${RETENTION_DAYS} -exec rm -rf {} \; 2>/dev/null

log "===== Backup Completed ====="
log "Total size:"
du -sh "${BACKUP_DIR}"
