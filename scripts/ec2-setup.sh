#!/bin/bash

# EC2 초기 설정 스크립트
# Ubuntu 22.04 LTS 기준

set -e  # 에러 발생 시 스크립트 중단

echo "========================================="
echo "EC2 Instance Setup Started"
echo "========================================="

# 1. 시스템 업데이트
echo "[1/7] Updating system packages..."
sudo apt-get update
sudo apt-get upgrade -y

# 2. Docker 설치
echo "[2/7] Installing Docker..."
if ! command -v docker &> /dev/null; then
    # Docker GPG 키 추가
    sudo apt-get install -y ca-certificates curl gnupg
    sudo install -m 0755 -d /etc/apt/keyrings
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
    sudo chmod a+r /etc/apt/keyrings/docker.gpg

    # Docker 저장소 추가
    echo \
      "deb [arch="$(dpkg --print-architecture)" signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
      "$(. /etc/os-release && echo "$VERSION_CODENAME")" stable" | \
      sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

    # Docker 설치
    sudo apt-get update
    sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

    # 현재 사용자를 docker 그룹에 추가
    sudo usermod -aG docker $USER
    echo "Docker installed successfully!"
else
    echo "Docker already installed, skipping..."
fi

# 3. Docker Compose 버전 확인
echo "[3/7] Checking Docker Compose version..."
docker compose version

# 4. 애플리케이션 디렉토리 생성
echo "[4/7] Creating application directory..."
APP_DIR="/home/$USER/concert-app"
mkdir -p $APP_DIR
cd $APP_DIR

# 5. .env 파일 생성 (템플릿)
echo "[5/7] Creating .env template..."
cat > .env.template << 'EOF'
# Docker Hub
DOCKERHUB_USERNAME=your-dockerhub-username

# MySQL
MYSQL_ROOT_PASSWORD=your-strong-root-password
MYSQL_DATABASE=concert_db
MYSQL_USER=concert_user
MYSQL_PASSWORD=your-strong-password

# Redis
REDIS_PASSWORD=your-strong-redis-password

# Spring
SPRING_PROFILES_ACTIVE=prod

# Service Ports
CORE_SERVICE_PORT=8080
QUEUE_SERVICE_PORT=8081
EOF

echo ".env template created at $APP_DIR/.env.template"
echo "Please copy .env.template to .env and update the values!"

# 6. docker-compose.prod.yml 다운로드 (GitHub에서)
echo "[6/7] Downloading docker-compose.prod.yml..."
echo "Please manually upload docker-compose.prod.yml to $APP_DIR"
echo "Or run: scp docker-compose.prod.yml user@ec2-host:$APP_DIR/docker-compose.yml"

# 7. 방화벽 설정 (UFW)
echo "[7/7] Configuring firewall..."
if command -v ufw &> /dev/null; then
    sudo ufw allow 22/tcp    # SSH
    sudo ufw allow 8080/tcp  # Core Service
    sudo ufw allow 8081/tcp  # Queue Service
    sudo ufw allow 80/tcp    # HTTP (Optional - for reverse proxy)
    sudo ufw allow 443/tcp   # HTTPS (Optional - for reverse proxy)
    echo "Firewall rules added. Run 'sudo ufw enable' to activate."
else
    echo "UFW not found, skipping firewall configuration..."
fi

echo "========================================="
echo "EC2 Instance Setup Completed!"
echo "========================================="
echo ""
echo "Next Steps:"
echo "1. Create .env file: cp .env.template .env && nano .env"
echo "2. Upload docker-compose.prod.yml to $APP_DIR/docker-compose.yml"
echo "3. Login to Docker Hub: docker login"
echo "4. Start services: docker compose up -d"
echo "5. Check status: docker compose ps"
echo ""
echo "Note: You may need to re-login for docker group changes to take effect."
echo "Run: newgrp docker"
