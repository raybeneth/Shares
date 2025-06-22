// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

contract TestStorageLayout {
    address public ownerAddress;
    bool public paused;

    constructor(address _ownerAddress, bool _paused) {
        ownerAddress = _ownerAddress;
        paused = _paused;
    }

    function setOwnerAddress(address _owner) external {
        ownerAddress = _owner;
    }

    function setPaused(bool _paused) external {
        paused = _paused;
    }
}
