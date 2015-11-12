#!/bin/bash

CPU_SHARES=#{CPU_SHARE}
DOCKER_ID=docker-$(docker inspect -f  '{{ .Id }}' #{DOCKER_NAME}).scope

OLD_SHARES=$(cat /sys/fs/cgroup/cpu/system.slice/$DOCKER_ID/cpu.shares)

sudo systemctl set-property $DOCKER_ID CPUShares=$CPU_SHARES

NEW_SHARES=$(cat /sys/fs/cgroup/cpu/system.slice/$DOCKER_ID/cpu.shares)

echo 'old shares: ' $OLD_SHARES ' new shares: ' $NEW_SHARES
