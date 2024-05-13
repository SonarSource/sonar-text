# Default RE constants that map to CI capabilities provided by Cirrus CI or RE Team
# Sonar Text does not not use Windows machines, so the default values are for Linux machines
RE_JAVA_17_IMAGE = "${CIRRUS_AWS_ACCOUNT}.dkr.ecr.eu-central-1.amazonaws.com/base:j17-latest"
RE_DEFAULT_REGION = "eu-central-1"
RE_SMALL_INSTANCE_TYPE = "t3.small"
RE_LARGE_INSTANCE_TYPE = "c5.4xlarge"


# SHARED CANDIDATE
def base_image_container_builder(
    cpu=4,
    memory="8G",
    image=RE_JAVA_17_IMAGE,
    use_in_memory_disk=True
):
    """
    Base configuration for a container that uses a pre-defined image provided by RE Team.

    Provides the default values for the container configuration:
    - image: the image to use for the container by default RE_JAVA_17_IMAGE
    - cluster_name: the name of the EKS cluster to use, by default "${CIRRUS_CLUSTER_NAME}"
    - region: the region of the EKS cluster, by default "eu-central-1"
    - namespace: the namespace to use for the container, by default "default"
    - use_in_memory_disk: whether to use an in-memory disk for the container, by default True
    - cpu: the number of CPUs to use for the container, by default 4
    - memory: the amount of memory to use for the container, by default "8G"
    """
    return {
        "image": image,
        "cluster_name": "${CIRRUS_CLUSTER_NAME}",
        "region": RE_DEFAULT_REGION,
        "namespace": "default",
        "use_in_memory_disk": use_in_memory_disk,
        "cpu": cpu,
        "memory": memory,
    }
