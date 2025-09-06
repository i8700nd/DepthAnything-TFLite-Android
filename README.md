# Depth-Anything - Android Demo With TensorFlow Lite

> An Android app inferencing the popular [Depth-Anything](https://arxiv.org/abs/2401.10891) model, which is used for monocular depth estimation

<table>
<tr>
<td>
<img width="338" height="691" alt="img2" src="https://github.com/user-attachments/assets/880b8871-ca6d-4ffa-b8bf-5cf46340d2fa" />
</td>
<td>
<img width="337" height="695" alt="img1" src="https://github.com/user-attachments/assets/7ada5f38-fcc6-4c80-ae9d-7e47e5486855" />
</td>
</tr>
</table>

## Project Setup

1. Download the TensorFlow Lite models from [HuggingFace](https://huggingface.co/qualcomm/Depth-Anything-V2) and place them in the `app/src/main/assets` directory.

2. [Connect a device](https://developer.android.com/codelabs/basic-android-kotlin-compose-connect-device#0) to Android Studio, and select `Run Application` from the top navigation pane.


## Citation

This App is using TensorFlow lite derived from shubham0204's Onnx Demo.
[shubham0204 Depth-Anything-Android](https://github.com/shubham0204/Depth-Anything-Android)

```
@misc{yang2024depth,
      title={Depth Anything V2}, 
      author={Lihe Yang and Bingyi Kang and Zilong Huang and Zhen Zhao and Xiaogang Xu and Jiashi Feng and Hengshuang Zhao},
      year={2024},
      eprint={2406.09414},
      archivePrefix={arXiv},
      primaryClass={id='cs.CV' full_name='Computer Vision and Pattern Recognition' is_active=True alt_name=None in_archive='cs' is_general=False description='Covers image processing, computer vision, pattern recognition, and scene understanding. Roughly includes material in ACM Subject Classes I.2.10, I.4, and I.5.'}
}
```

```
@article{depthanything,
      title={Depth Anything: Unleashing the Power of Large-Scale Unlabeled Data}, 
      author={Yang, Lihe and Kang, Bingyi and Huang, Zilong and Xu, Xiaogang and Feng, Jiashi and Zhao, Hengshuang},
      journal={arXiv:2401.10891},
      year={2024}
}
```

```
@misc{oquab2023dinov2,
  title={DINOv2: Learning Robust Visual Features without Supervision},
  author={Oquab, Maxime and Darcet, Timothée and Moutakanni, Theo and Vo, Huy V. and Szafraniec, Marc and Khalidov, Vasil and Fernandez, Pierre and Haziza, Daniel and Massa, Francisco and El-Nouby, Alaaeldin and Howes, Russell and Huang, Po-Yao and Xu, Hu and Sharma, Vasu and Li, Shang-Wen and Galuba, Wojciech and Rabbat, Mike and Assran, Mido and Ballas, Nicolas and Synnaeve, Gabriel and Misra, Ishan and Jegou, Herve and Mairal, Julien and Labatut, Patrick and Joulin, Armand and Bojanowski, Piotr},
  journal={arXiv:2304.07193},
  year={2023}
}
```
