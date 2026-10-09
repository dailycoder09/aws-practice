#!/bin/bash
# EC2 user data for Amazon Linux 2023. Paste this into "Advanced details > User data" when launching
# each web instance. It installs Docker, builds the three apps from GitHub and starts them.
#
# Containers use host networking, so each app listens on its own port on the instance and the
# services reach each other on localhost (product-service -> localhost:8082 and the other way round).
# No service URLs need to be configured.
#
#   ui-app             8080   (target group health check: /healthz)
#   product-service    8081   (health check: /actuator/health)
#   inventory-service  8082   (health check: /actuator/health)
#
# Follow the build with:  sudo tail -f /var/log/user-data.log
set -euo pipefail
exec > /var/log/user-data.log 2>&1

echo "===== USER DATA STARTED ====="

# Maven and npm builds need more memory than a small instance has: add 2 GB of swap.
if ! swapon --show | grep -q /swapfile; then
  dd if=/dev/zero of=/swapfile bs=1M count=2048
  chmod 600 /swapfile
  mkswap /swapfile
  swapon /swapfile
  echo "Swap enabled"
fi

dnf update -y
dnf install -y git docker

systemctl enable docker
systemctl start docker
echo "Docker started"

usermod -aG docker ec2-user

cd /home/ec2-user
git clone https://github.com/dailycoder09/aws-practice.git
cd aws-practice
echo "Repository cloned"

build_and_run() {
  local name="$1" dir="$2"
  echo "===== $name ====="
  docker build -t "$name:1.0" "$dir"
  docker rm -f "$name-container" >/dev/null 2>&1 || true
  docker run -d \
    --name "$name-container" \
    --restart unless-stopped \
    --network host \
    "$name:1.0"
  echo "$name started"
}

build_and_run product-service   product-service
build_and_run inventory-service inventory-service
build_and_run ui-app            ui-app

echo "===== DOCKER IMAGES ====="
docker images
echo "===== DOCKER CONTAINERS ====="
docker ps -a
echo "===== USER DATA COMPLETED ====="
