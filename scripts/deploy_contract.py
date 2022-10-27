# Copyright 2021 ICON Foundation
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

from iconsdk.libs.in_memory_zip import gen_deploy_data_content

from scripts.config import Config

JAVA_CONTRACTS = {
    'app': '0.1.0',
}


def print_empty(*args):
    pass


def deploy(config: Config, target: str, to=None, params: dict = None, verbose=print_empty):
    owner = config.owner
    tx_handler = config.tx_handler
    verbose(">>> owner address:", owner.get_address())

    version = JAVA_CONTRACTS.get(target)
    if version:
        jar_path = f'./{target}/build/libs/{target}-{version}-optimized.jar'
        verbose(">>> jar path =", jar_path)
        content = gen_deploy_data_content(jar_path)
        ctype = 'application/java'
    else:
        score_path = f"./{target}"
        content = gen_deploy_data_content(score_path)
        ctype = 'application/zip'
    verbose(">>> content size =", len(content))
    if to is None:
        tx_hash = tx_handler.install(owner, content, ctype, params)
    else:
        verbose(">>> to =", to)
        tx_hash = tx_handler.update(owner, to, content, ctype, params)
    verbose(">>> deploy txHash:", tx_hash)

    tx_result = tx_handler.ensure_tx_result(tx_hash, verbose != print_empty)
    score_address = tx_result["scoreAddress"]
    verbose(">>> scoreAddress:", score_address)
    return score_address
